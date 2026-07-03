package dev.defdo.mobile.auth

import org.junit.Test

class PKCETestJUnit {
    @Test fun testAll() { runPKCETest() }
}

class AuthorizationRequestBuilderTestJUnit {
    @Test fun testAll() { runAuthorizationRequestBuilderTest() }
}

class CallbackValidatorTestJUnit {
    @Test fun testAll() { runCallbackValidatorTest() }
}

class DiscoveryContractTestJUnit {
    @Test fun testAll() { runDiscoveryContractTest() }
}

class TokenResponseContractTestJUnit {
    @Test fun testAll() { runTokenResponseContractTest() }
}

class RefreshContractTestJUnit {
    @Test fun testAll() { runRefreshContractTest() }
}

class RevokeLogoutContractTestJUnit {
    @Test fun testAll() { runRevokeLogoutContractTest() }
}

class AuthErrorNormalizerTestJUnit {
    @Test fun testAll() { runAuthErrorNormalizerTest() }
}

class AuthRedactorTestJUnit {
    @Test fun testAll() { runAuthRedactorTest() }
}

class PlatformAdapterTestJUnit {
    @Test fun testPlatformAdapter() { runPlatformAdapterTest() }
    @Test fun testTokenStoreAdapter() { runTokenStoreAdapterTest() }
    @Test fun testCallbackHandoff() { runCallbackHandoffTest() }
    @Test fun testTokenEnvelopeCorrupt() { runTokenEnvelopeCorruptTest() }
    @Test fun testTokenEnvelopeFutureSchema() { runTokenEnvelopeFutureSchemaTest() }
    @Test fun testTokenEnvelopeMissingOptionals() { runTokenEnvelopeMissingOptionalsTest() }
    @Test fun testTokenTypeEnforcement() { runTokenTypeEnforcementTest() }
    @Test fun testTokenTypeBearerPersists() { runTokenTypeBearerPersistsTest() }
    @Test fun testTokenTypeAbsentPersists() { runTokenTypeAbsentPersistsTest() }
    @Test fun testDiscoveryHardeningNotes() { runDiscoveryHardeningNotes() }
}
