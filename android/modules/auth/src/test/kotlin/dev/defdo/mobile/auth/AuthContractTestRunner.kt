package dev.defdo.mobile.auth

fun main() {
    try {
        runPKCETest()
        println("  [PASS] PKCETest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] PKCETest: ${e.message}")
        throw e
    }
    try {
        runAuthorizationRequestBuilderTest()
        println("  [PASS] AuthorizationRequestBuilderTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] AuthorizationRequestBuilderTest: ${e.message}")
        throw e
    }
    try {
        runCallbackValidatorTest()
        println("  [PASS] CallbackValidatorTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] CallbackValidatorTest: ${e.message}")
        throw e
    }
    try {
        runDiscoveryContractTest()
        println("  [PASS] DiscoveryContractTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] DiscoveryContractTest: ${e.message}")
        throw e
    }
    try {
        runTokenResponseContractTest()
        println("  [PASS] TokenResponseContractTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenResponseContractTest: ${e.message}")
        throw e
    }
    try {
        runRefreshContractTest()
        println("  [PASS] RefreshContractTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] RefreshContractTest: ${e.message}")
        throw e
    }
    try {
        runRevokeLogoutContractTest()
        println("  [PASS] RevokeLogoutContractTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] RevokeLogoutContractTest: ${e.message}")
        throw e
    }
    try {
        runAuthErrorNormalizerTest()
        println("  [PASS] AuthErrorNormalizerTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] AuthErrorNormalizerTest: ${e.message}")
        throw e
    }
    try {
        runAuthRedactorTest()
        println("  [PASS] AuthRedactorTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] AuthRedactorTest: ${e.message}")
        throw e
    }
    try {
        runPlatformAdapterTest()
        println("  [PASS] PlatformAdapterTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] PlatformAdapterTest: ${e.message}")
        throw e
    }
    try {
        runTokenStoreAdapterTest()
        println("  [PASS] TokenStoreAdapterTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenStoreAdapterTest: ${e.message}")
        throw e
    }
    try {
        runCallbackHandoffTest()
        println("  [PASS] CallbackHandoffTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] CallbackHandoffTest: ${e.message}")
        throw e
    }
    try {
        runTokenEnvelopeCorruptTest()
        println("  [PASS] TokenEnvelopeCorruptTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenEnvelopeCorruptTest: ${e.message}")
        throw e
    }
    try {
        runTokenEnvelopeFutureSchemaTest()
        println("  [PASS] TokenEnvelopeFutureSchemaTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenEnvelopeFutureSchemaTest: ${e.message}")
        throw e
    }
    try {
        runTokenEnvelopeMissingOptionalsTest()
        println("  [PASS] TokenEnvelopeMissingOptionalsTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenEnvelopeMissingOptionalsTest: ${e.message}")
        throw e
    }
    try {
        runTokenTypeEnforcementTest()
        println("  [PASS] TokenTypeEnforcementTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenTypeEnforcementTest: ${e.message}")
        throw e
    }
    try {
        runTokenTypeBearerPersistsTest()
        println("  [PASS] TokenTypeBearerPersistsTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenTypeBearerPersistsTest: ${e.message}")
        throw e
    }
    try {
        runTokenTypeAbsentPersistsTest()
        println("  [PASS] TokenTypeAbsentPersistsTest")
    } catch (e: Exception) {
        System.err.println("  [FAIL] TokenTypeAbsentPersistsTest: ${e.message}")
        throw e
    }
    try {
        runDiscoveryHardeningNotes()
        println("  [PASS] DiscoveryHardeningNotes")
    } catch (e: Exception) {
        System.err.println("  [FAIL] DiscoveryHardeningNotes: ${e.message}")
        throw e
    }
    println()
    println("android auth contract tests passed")
}
