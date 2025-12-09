package com.forestfull.chat.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.forestfull.common.token.JwtUtil;
import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.nio.file.attribute.UserPrincipal;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        Object tokenPayloadObject = accessor.getSessionAttributes().get(JwtUtil.TOKEN_TYPE.JWT_PAYLOAD.name());

        if (tokenPayloadObject != null) {
            String payload = tokenPayloadObject.toString();
            try {
                jwtUtil.verifyToken(payload);
                String json = new String(Base64.getDecoder().decode(payload));
                JSONObject obj = new JSONObject(json);

                String username = obj.getString("username");
                String displayName = obj.optString("displayName", username);
                String profileImage = obj.optString("profileImage", null);

                accessor.setUser(User.builder()
                        .id(Long.parseLong(obj.getString("sub")))
                        .name(username)
                        .displayName(displayName)
                        .profileImage(profileImage)
                        .build());

            } catch (Exception ignored) {
            }
        }

        return message;
    }
}