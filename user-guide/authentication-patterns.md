# Authentication Testing Patterns

<!-- AI-METADATA
test-types: [component, pagemodel, jaxrs]
patterns: [authentication, session, principal, roles, tiers]
keywords: [login, logout, user, session, principal, mock, security, authorization]
difficulty: intermediate
-->

## Overview

This guide covers patterns for testing authenticated user scenarios in BRUT, including:
- Fluent authentication API (recommended)
- Principal-based authentication
- Session-based user objects
- Role/tier-based access control
- Helper methods for cleaner tests

## Fluent Authentication API (Recommended)

BRUT 5.1.0+ provides fluent methods for authentication directly on the request builder:

```java
@BrxmJaxrsTest(beanPackages = {"org.example.model"})
class SecureApiTest {
    private DynamicJaxrsTest brxm;

    @Test
    void authenticatedUser_canAccessProtectedEndpoint() {
        String response = brxm.request()
            .get("/site/api/protected")
            .asUser("john", "admin", "editor")  // username + roles
            .execute();

        assertThat(response).contains("Welcome, john");
    }

    @Test
    void adminRole_canAccessDashboard() {
        String response = brxm.request()
            .get("/site/api/dashboard")
            .withRole("admin")  // role-only (no username)
            .execute();

        assertThat(response).contains("dashboard");
    }
}
```

### Method Reference

| Method | Description |
|--------|-------------|
| `asUser(username, roles...)` | Sets remote user and roles |
| `withRole(roles...)` | Sets roles without username |

### When to Use Each Method

**Use `asUser()` when:**
- Your code checks `request.getRemoteUser()` or principal name
- You need to identify the specific user in tests
- Testing personalized content or user-specific features

**Use `withRole()` when:**
- Your code only checks roles via `request.isUserInRole()`
- Testing role-based access control without user identity
- Simpler tests that don't need full user context

### Fluent Auth with Page Model API

```java
@BrxmPageModelTest(loadProjectContent = true)
class SecurePageModelTest {
    private DynamicPageModelTest brxm;

    @Test
    void premiumUser_seesExclusiveContent() throws Exception {
        PageModelResponse pageModel = brxm.request()
            .get("/site/resourceapi/premium")
            .asUser("subscriber", "premium-tier")
            .executeAsPageModel();

        assertThat(pageModel.findComponentByName("ExclusiveContent")).isPresent();
    }
}
```

## Testing Authentication Failures

Use `brxm.authentication()` to configure mock credential rejection for testing login failures:

```java
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {AuthResource.class}
)
class AuthResourceTest {
    private DynamicJaxrsTest brxm;

    @Test
    void login_fails_forInvalidUser() {
        brxm.authentication().rejectUser("unknown");

        String response = brxm.request()
            .post("/site/api/auth/login")
            .withBody("{\"username\":\"unknown\",\"password\":\"any\"}")
            .execute();

        assertThat(response).contains("401");
    }

    @Test
    void login_fails_forWrongPassword() {
        brxm.authentication().rejectPassword("wrongpassword");

        String response = brxm.request()
            .post("/site/api/auth/login")
            .withBody("{\"username\":\"john\",\"password\":\"wrongpassword\"}")
            .execute();

        assertThat(response).contains("Invalid credentials");
    }

    @Test
    void login_fails_forLockedAccount() {
        brxm.authentication()
            .rejectUser("locked")
            .rejectUser("disabled");

        // Both users will fail authentication
    }
}
```

### Mock Authentication Methods

| Method | Description |
|--------|-------------|
| `rejectUser(username)` | Reject credentials with this username |
| `rejectPassword(password)` | Reject credentials with this password |

Methods are chainable and automatically reset between tests.

### Use Cases

- **Invalid credentials** - Test 401 responses
- **Non-existent users** - Test user lookup failures
- **Locked/disabled accounts** - Test account status checks
- **Password validation** - Test wrong password handling

## Manual Authentication Approaches

For more complex scenarios (session objects, custom principals), use the manual approaches below.

## Principal-Based Authentication

The simplest authentication pattern uses `HttpServletRequest.getUserPrincipal()`:

```java
@BrxmComponentTest(beanPackages = {"com.example.beans"})
class LoginComponentTest {

    private DynamicComponentTest brxm;
    private LoginComponent component;

    @BeforeEach
    void setUp() {
        component = new LoginComponent();
        component.init(null, brxm.getComponentConfiguration());
    }

    private void setLoggedInUser(String username) {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(username);
        brxm.getHstRequest().setUserPrincipal(principal);
    }

    @Test
    void doBeforeRender_whenNotLoggedIn_setsLoggedInFalse() {
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        Boolean loggedIn = brxm.getRequestAttributeValue("loggedin");
        assertThat(loggedIn).isFalse();
    }

    @Test
    void doBeforeRender_whenLoggedIn_setsLoggedInTrue() {
        setLoggedInUser("testuser");

        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        Boolean loggedIn = brxm.getRequestAttributeValue("loggedin");
        assertThat(loggedIn).isTrue();
    }
}
```

## Session-Based Authentication

For applications storing user data in HTTP sessions:

```java
private void setSession(HttpSession session) {
    brxm.getHstRequest().setSession(session);
}

@Test
void doBeforeRender_withUserProfile_createsUserFromProfile() {
    setLoggedInUser("john");

    UserProfile userProfile = new UserProfile();
    userProfile.setFirstname("John");
    userProfile.setLastname("Doe");
    userProfile.setGroups(Arrays.asList("everybody", "premium-tier"));

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(null);
    when(session.getAttribute("userProfile")).thenReturn(userProfile);
    setSession(session);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    UserModel userModel = brxm.getRequestAttributeValue("user");
    assertThat(userModel.getFirstname()).isEqualTo("John");
    assertThat(userModel.getLastname()).isEqualTo("Doe");
}

@Test
void doBeforeRender_withExistingSessionUser_usesSessionUser() {
    setLoggedInUser("jane");

    User existingUser = new User("Jane", "Smith", Arrays.asList("everybody", "admin"));

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(existingUser);
    setSession(session);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    UserModel userModel = brxm.getRequestAttributeValue("user");
    assertThat(userModel.getFirstname()).isEqualTo("Jane");
}
```

## Role-Based Helper Methods

Create tier/role helper methods for cleaner tests:

```java
@BrxmPageModelTest
class DashboardPageModelApiTest {

    private DynamicPageModelTest brxm;

    // Authentication helper with full user setup
    private void authenticateUser(String username, String firstname,
                                   String lastname, List<String> groups) {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(username);
        brxm.getHstRequest().setUserPrincipal(principal);

        User user = new User(firstname, lastname, groups);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(user);
        brxm.getHstRequest().setSession(session);
    }

    // Role-specific convenience methods
    private void authenticateAsFreeUser() {
        authenticateUser("freeuser", "Free", "User",
            Arrays.asList("everybody", "petbase-free"));
    }

    private void authenticateAsEssentialUser() {
        authenticateUser("essentialuser", "Essential", "User",
            Arrays.asList("everybody", "petbase-essential"));
    }

    private void authenticateAsAdventureUser() {
        authenticateUser("adventureuser", "Adventure", "User",
            Arrays.asList("everybody", "petbase-adventure"));
    }

    @Test
    void freeTier_userSeesDashboard() throws Exception {
        authenticateAsFreeUser();

        PageModelResponse pageModel = brxm.request()
            .get("/site/resourceapi/dashboard")
            .executeAsPageModel();

        assertThat(pageModel).isNotNull();
        assertThat(pageModel.getRootComponent()).isNotNull();
    }

    @Test
    void essentialTier_userHasLocationMap() throws Exception {
        authenticateAsEssentialUser();

        PageModelResponse pageModel = brxm.request()
            .get("/site/resourceapi/dashboard")
            .executeAsPageModel();

        PageComponent locationMap = pageModel.findComponentByName("LocationMap").orElseThrow();
        assertThat(locationMap.getComponentClass())
            .isEqualTo("com.example.components.LocationMapComponent");
    }
}
```

## Testing Subscription Tiers

Pattern for testing tier-based access control:

```java
@Test
void doBeforeRender_withFreeSubscription_hasCorrectAccess() {
    setLoggedInUser("freeuser");

    User freeUser = new User("Free", "User", Arrays.asList("everybody", "petbase-free"));

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(freeUser);
    setSession(session);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    String subscriptionTier = brxm.getRequestAttributeValue("subscriptionTier");
    assertThat(subscriptionTier).isEqualTo("free");

    Boolean hasEssentialAccess = brxm.getRequestAttributeValue("hasEssentialAccess");
    assertThat(hasEssentialAccess).isFalse();

    Boolean hasAdventureAccess = brxm.getRequestAttributeValue("hasAdventureAccess");
    assertThat(hasAdventureAccess).isFalse();
}

@Test
void doBeforeRender_withAdventureTier_hasFullAccess() {
    setLoggedInUser("adventureuser");

    User adventureUser = new User("Adventure", "User",
        Arrays.asList("everybody", "petbase-adventure"));

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(adventureUser);
    setSession(session);

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    String subscriptionTier = brxm.getRequestAttributeValue("subscriptionTier");
    assertThat(subscriptionTier).isEqualTo("adventure");

    Boolean hasAdventureAccess = brxm.getRequestAttributeValue("hasAdventureAccess");
    assertThat(hasAdventureAccess).isTrue();
}
```

## Testing Request Parameters

Components often check for login/error query parameters:

```java
@Test
void doBeforeRender_withLoginParameter_setsLoginModelTrue() {
    brxm.addRequestParameter("login", "true");

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    Boolean login = brxm.getRequestAttributeValue("login");
    assertThat(login).isTrue();
}

@Test
void doBeforeRender_withErrorParameter_setsErrorModelTrue() {
    brxm.addRequestParameter("error", "true");

    component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

    Boolean error = brxm.getRequestAttributeValue("error");
    assertThat(error).isTrue();
}
```

## Authentication Test Coverage Checklist

Ensure your authentication tests cover:

- [ ] **Unauthenticated state** - No principal, no session
- [ ] **Principal only** - Principal set, no session user
- [ ] **Session user** - Full user object in session
- [ ] **User profile** - External profile (e.g., from SSO/SAML)
- [ ] **Each subscription tier** - Free, basic, premium, etc.
- [ ] **Role-based access** - Admin, editor, viewer permissions
- [ ] **Login parameters** - `?login=true`, `?error=true`
- [ ] **Session expiry** - Invalidated or expired sessions
- [ ] **Invalid credentials** - Wrong username/password (use `brxm.authentication()`)
- [ ] **Locked accounts** - Disabled or suspended users

## PageModel API Authentication Pattern

For Page Model API tests with authentication:

```java
@BrxmPageModelTest
class SecurePageModelTest {

    private DynamicPageModelTest brxm;

    private PageModelResponse fetchPageModel(String path) throws Exception {
        return brxm.request()
            .get("/site/resourceapi" + path)
            .executeAsPageModel();
    }

    @Test
    void unauthenticated_dashboardShowsPromotionalContent() throws Exception {
        PageModelResponse pageModel = fetchPageModel("/dashboard");

        PageComponent heroBanner = pageModel.findComponentByName("HeroBanner").orElseThrow();
        Map<String, Object> model = heroBanner.getModel("heroBanner");

        HeroBannerContent content = MAPPER.convertValue(model, HeroBannerContent.class);
        assertThat(content.getCallToAction().getTitle()).isEqualTo("Get Started Free");
    }

    @Test
    void authenticated_dashboardShowsPersonalizedContent() throws Exception {
        authenticateAsEssentialUser();

        PageModelResponse pageModel = fetchPageModel("/dashboard");

        assertThat(pageModel.findComponentByName("PetProfile")).isPresent();
        assertThat(pageModel.findComponentByName("SubscriptionStatus")).isPresent();
    }
}
```

## Test Naming Convention

Use descriptive test names that indicate authentication state:

```java
// Good: Clear authentication context
@Test void unauthenticated_dashboardRedirectsToLogin()
@Test void freeTier_userSeesUpgradeBanner()
@Test void essentialTier_userHasLocationTracking()
@Test void adventureTier_userHasAllFeatures()

// Avoid: Ambiguous about auth state
@Test void testDashboard()
@Test void dashboardWorks()
```

## Reusable Authentication Test Base

Consider creating a base class with auth helpers:

```java
public abstract class AuthenticatedComponentTestBase {

    protected abstract DynamicComponentTest getBrxm();

    protected void setLoggedInUser(String username) {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(username);
        getBrxm().getHstRequest().setUserPrincipal(principal);
    }

    protected void authenticateWithTier(String tier) {
        setLoggedInUser(tier + "user");
        User user = new User(tier, "User", Arrays.asList("everybody", "myapp-" + tier));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(user);
        getBrxm().getHstRequest().setSession(session);
    }

    protected void logout() {
        getBrxm().getHstRequest().setUserPrincipal(null);
        getBrxm().getHstRequest().setSession(null);
    }
}
```

## Related Guides

- [Common Test Patterns](common-patterns.md) - General testing patterns
- [JAX-RS Testing](jaxrs-testing.md) - Testing REST endpoints with auth
- [Troubleshooting](troubleshooting.md) - Common auth testing issues
