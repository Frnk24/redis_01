/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Filter.java to edit this template
 */
package controlador;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// La anotación @WebFilter le dice a Tomcat que este filtro se aplica a TODAS las peticiones ("/*")
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    public AuthFilter() {
    }

    @Override
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
    
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    
    String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
    
    // Páginas públicas que no requieren login
    boolean isPublicPage = path.equals("/index.html") || 
                           path.equals("/LoginServlet") ||
                           path.equals("/registro.html") || 
                           path.equals("/RegistroServlet") ||
                           path.startsWith("/img/") ||
                           path.startsWith("/recursos/"); // Asumiendo que guardas CSS/JS en /recursos/

    if (isPublicPage) {
        // Si es una página pública, dejamos pasar sin verificar nada más.
        chain.doFilter(request, response);
        return; // Importante: salimos del método para no seguir evaluando.
    }

    // A partir de aquí, todas las páginas son protegidas.
    // Verificamos si hay una sesión.
    HttpSession session = httpRequest.getSession(false);
    
    if (session == null || session.getAttribute("usuario") == null) {
        // No hay sesión o no hay usuario en la sesión, redirigir al login.
        System.out.println("FILTRO: Intento de acceso a página protegida sin sesión. Redirigiendo a login.");
        httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html");
        return;
    }
    
    
    modelo.Usuarios usuario = 
            (modelo.Usuarios) session.getAttribute("usuario");
    String userRole = usuario.getRol(); 
    
    
if (path.equals("/admin.html")) {
    if ("admin".equals(userRole)) {
        
        chain.doFilter(request, response);
    } else {
        
        System.out.println("FILTRO: Acceso DENEGADO para el rol '" + userRole + "' a la página " + path);
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes permiso para acceder a esta página.");
    }
} 

else if (path.equals("/admin/productos") && httpRequest.getMethod().equalsIgnoreCase("POST")) {
     if ("admin".equals(userRole)) {
       
        chain.doFilter(request, response);
    } else {
        
        System.out.println("FILTRO: Acceso DENEGADO para el rol '" + userRole + "' para modificar productos.");
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes permiso para realizar esta acción.");
    }
}
else {
    
    chain.doFilter(request, response);
}
}

    @Override
    public void destroy() {        
    }

    @Override
    public void init(FilterConfig filterConfig) {        
    }
}
