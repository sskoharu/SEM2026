package org.app.security;

import java.util.Optional;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionAuthorizationFilterTest {
    @Test
    void uuidAloneAndAnotherSessionTokenNeverAuthorizeSensitiveRoutes() throws Exception {
        SessionRepository repository = mock(SessionRepository.class);
        SessionObject owner = new SessionObject();
        owner.setSessionId("11111111-1111-1111-1111-111111111111");
        String valid = SessionAccess.issue(owner);
        String other = SessionAccess.issue(new SessionObject());
        when(repository.findById(owner.getSessionId())).thenReturn(Optional.of(owner));
        var filter = new SessionAuthorizationFilter(repository);
        for (String route : new String[] {
                "/accept/", "/evaluate/", "/batch-update/", "/select-action/", "/submit-procedural-form/" }) {
            for (String token : new String[] { "", other }) {
                var request = new MockHttpServletRequest("POST", "/api/v1/claims" + route + owner.getSessionId());
                request.addHeader(SessionAccess.HEADER, token);
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (req, res) -> fail("Unauthorized call reached controller"));
                assertEquals(403, response.getStatus());
            }
        }
        for (String suffix : new String[] { "/layer1/clarify", "/layer2/start", "/layer2/answer",
                "/layer4/calculate", "/layer5/preview", "/layer5/generate" }) {
            var denied = new MockHttpServletRequest("POST", "/api/v1/claims/" + owner.getSessionId() + suffix);
            var deniedResponse = new MockHttpServletResponse();
            filter.doFilter(denied, deniedResponse, (req, res) -> fail("UUID is not authorization"));
            assertEquals(403, deniedResponse.getStatus());
            denied.addHeader(SessionAccess.HEADER, valid);
            var accepted = new MockHttpServletResponse();
            filter.doFilter(denied, accepted, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
            assertEquals(204, accepted.getStatus());
        }
        assertFalse(SessionAccess.permits(owner, other));
        assertTrue(SessionAccess.permits(owner, valid));
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(owner).contains(owner.getAccessTokenHash()));
    }
}
