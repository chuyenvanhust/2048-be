package com.example.demo.game2048.backend.config; // CHÚ Ý: Sửa lại cho đúng package của bạn

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Kích hoạt cấu hình CORS bên dưới
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Tắt CSRF bảo mật để có thể gọi được các API POST, PUT, DELETE từ bên ngoài
                .csrf(csrf -> csrf.disable())

                // 3. Cấu hình phân quyền (Cho phép tất cả truy cập vào API để test game)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Cho phép các yêu cầu OPTIONS (Preflight)
                        .requestMatchers("/api/**").permitAll()              // Cho phép tất cả API bắt đầu bằng /api/
                        .anyRequest().permitAll()                            // Cho phép tất cả các yêu cầu còn lại
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // QUAN TRỌNG: Thêm cả link localhost và link Render của bạn vào đây
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",           // Cho phép khi bạn chạy ở máy cá nhân
                "https://two048-fe-1.onrender.com" // Cho phép link Frontend trên Render
        ));

        // Cho phép các phương thức HTTP
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // QUAN TRỌNG: Phải liệt kê Header "X-Session-Id" để Spring không chặn nó
        config.setAllowedHeaders(List.of(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Session-Id",
                "X-Requested-With"
        ));

        // Cho phép gửi kèm Cookie hoặc thông tin xác thực nếu cần
        config.setAllowCredentials(true);

        // Áp dụng cấu hình trên cho tất cả các đường dẫn API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}