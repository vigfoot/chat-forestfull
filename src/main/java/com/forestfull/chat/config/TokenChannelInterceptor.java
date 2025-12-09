package com.forestfull.chat.config;

import com.forestfull.common.token.JwtUtil;
import com.forestfull.domain.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Object tokenObj = sessionAttributes.get(JwtUtil.TOKEN_TYPE.JWT.name());

        if (tokenObj != null) {
            String token = tokenObj.toString();
            try {
                Long userId = Long.valueOf(jwtUtil.verifyToken(token).getSubject());
                var userDetails = customUserDetailsService.loadUserByUserId(userId);
                var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception ignored) {
            }
        }

        return message;
    }
}