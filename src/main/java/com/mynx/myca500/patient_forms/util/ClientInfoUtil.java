package com.mynx.myca500.patient_forms.util;

import com.mynx.myca500.patient_forms.model.ClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ClientInfoUtil {

    private static final int MAX_USER_AGENT = 512;

    private ClientInfoUtil() {
    }

    public static ClientInfo from(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > MAX_USER_AGENT) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT);
        }
        return new ClientInfo(request.getRemoteAddr(), userAgent);
    }

    public static ClientInfo current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return from(attributes.getRequest());
        }
        return ClientInfo.empty();
    }
}
