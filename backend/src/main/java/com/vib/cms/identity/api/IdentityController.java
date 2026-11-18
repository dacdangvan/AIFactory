package com.vib.cms.identity.api;
import com.vib.cms.common.api.ApiResponse;import org.springframework.security.core.Authentication;import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/identity") public class IdentityController {
 @GetMapping("/me") public ApiResponse<Map<String,Object>> me(Authentication auth){if(auth instanceof JwtAuthenticationToken jwt)return ApiResponse.ok(Map.of("subject",jwt.getToken().getSubject(),"username",jwt.getName(),"claims",jwt.getToken().getClaims()));return ApiResponse.ok(Map.of("username",auth==null?"local-dev":auth.getName()));}
}
