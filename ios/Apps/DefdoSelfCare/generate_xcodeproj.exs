#!/usr/bin/env elixir
# Usage: elixir ios/Apps/DefdoSelfCare/generate_xcodeproj.exs
#
# Generates a minimal DefdoSelfCare.xcodeproj for the iOS app shell. The
# generated project targets iOS 16+, consumes the local SwiftPM package
# (DefdoSelfCareKit), and references Info.plist + DefdoSelfCare.entitlements.
#
# The project is intentionally minimal: no app icons, no launch storyboard, no
# signing team, and dev-only entitlements. Re-run this script after editing the
# package, Info.plist, or entitlements.

Mix.install([])

defmodule XcodeprojGenerator do
  @moduledoc false

  def run(argv \\ []) do
    root =
      case argv do
        [path] -> Path.expand(path)
        [] -> Path.dirname(__ENV__.file) |> Path.expand()
      end

    project_dir = Path.join(root, "DefdoSelfCare.xcodeproj")
    workspace_dir = Path.join(project_dir, "project.xcworkspace")
    shareddata_dir = Path.join(workspace_dir, "xcshareddata")

    File.mkdir_p!(shareddata_dir)

    ids = generate_ids()

    Path.join(project_dir, "project.pbxproj")
    |> File.write!(pbxproj(ids))

    Path.join(workspace_dir, "contents.xcworkspacedata")
    |> File.write!(contents_xcworkspacedata())

    Path.join(shareddata_dir, "WorkspaceSettings.xcsettings")
    |> File.write!(workspace_settings())

    IO.puts("Generated #{project_dir}")
  end

  defp generate_ids do
    keys = [
      :project,
      :main_group,
      :products_group,
      :project_config_list,
      :project_debug_config,
      :project_release_config,
      :app_target,
      :target_config_list,
      :app_product,
      :info_plist,
      :entitlements,
      :app_source,
      :source_build_file,
      :frameworks_phase,
      :app_group,
      :sources_phase,
      :resources_phase,
      :kit_product_dependency,
      :kit_product_proxy,
      :local_package_reference,
      :target_debug_config,
      :target_release_config
    ]

    Map.new(keys, fn key -> {key, uuid()} end)
  end

  defp uuid do
    <<a::32, b::16, c::16, d::16, e::48>> = :crypto.strong_rand_bytes(16)
    [hex(a, 8), hex(b, 4), hex(c, 4), hex(d, 4), hex(e, 12)]
    |> Enum.join()
    |> String.upcase()
  end

  defp hex(value, width) do
    value
    |> Integer.to_string(16)
    |> String.pad_leading(width, "0")
  end

  defp pbxproj(ids) do
    """
// !$*UTF8*$!
{
    archiveVersion = 1;
    classes = {
    };
    objectVersion = 77;
    objects = {

/* Begin PBXBuildFile section */
        #{ids[:source_build_file]} /* DefdoSelfCareApp.swift in Sources */ = {isa = PBXBuildFile; fileRef = #{ids[:app_source]} /* DefdoSelfCareApp.swift */; };
        #{ids[:kit_product_dependency]} /* DefdoSelfCareKit in Frameworks */ = {isa = PBXBuildFile; productRef = #{ids[:kit_product_proxy]} /* DefdoSelfCareKit */; };
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
        #{ids[:app_product]} /* DefdoSelfCare.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = DefdoSelfCare.app; sourceTree = BUILT_PRODUCTS_DIR; };
        #{ids[:info_plist]} /* Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; };
        #{ids[:entitlements]} /* DefdoSelfCare.entitlements */ = {isa = PBXFileReference; lastKnownFileType = text.plist.entitlements; path = DefdoSelfCare.entitlements; sourceTree = "<group>"; };
        #{ids[:app_source]} /* DefdoSelfCareApp.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; name = DefdoSelfCareApp.swift; path = ../Sources/DefdoSelfCare/DefdoSelfCareApp.swift; sourceTree = SOURCE_ROOT; };
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
        #{ids[:frameworks_phase]} /* Frameworks */ = {
            isa = PBXFrameworksBuildPhase;
            buildActionMask = 2147483647;
            files = (
                #{ids[:kit_product_dependency]} /* DefdoSelfCareKit in Frameworks */,
            );
            runOnlyForDeploymentPostprocessing = 0;
        };
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
        #{ids[:main_group]} = {
            isa = PBXGroup;
            children = (
                #{ids[:app_group]} /* DefdoSelfCare */,
                #{ids[:products_group]} /* Products */,
            );
            sourceTree = "<group>";
        };
        #{ids[:products_group]} /* Products */ = {
            isa = PBXGroup;
            children = (
                #{ids[:app_product]} /* DefdoSelfCare.app */,
            );
            name = Products;
            sourceTree = "<group>";
        };
        #{ids[:app_group]} /* DefdoSelfCare */ = {
            isa = PBXGroup;
            children = (
                #{ids[:info_plist]} /* Info.plist */,
                #{ids[:entitlements]} /* DefdoSelfCare.entitlements */,
                #{ids[:app_source]} /* DefdoSelfCareApp.swift */,
            );
            path = DefdoSelfCare;
            sourceTree = "<group>";
        };
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
        #{ids[:app_target]} /* DefdoSelfCare */ = {
            isa = PBXNativeTarget;
            buildConfigurationList = #{ids[:target_config_list]} /* Build configuration list for PBXNativeTarget "DefdoSelfCare" */;
            buildPhases = (
                #{ids[:sources_phase]} /* Sources */,
                #{ids[:frameworks_phase]} /* Frameworks */,
                #{ids[:resources_phase]} /* Resources */,
            );
            buildRules = (
            );
            dependencies = (
            );
            name = DefdoSelfCare;
            packageProductDependencies = (
                #{ids[:kit_product_proxy]} /* DefdoSelfCareKit */,
            );
            productName = DefdoSelfCare;
            productReference = #{ids[:app_product]} /* DefdoSelfCare.app */;
            productType = "com.apple.product-type.application";
        };
/* End PBXNativeTarget section */

/* Begin PBXProject section */
        #{ids[:project]} /* Project object */ = {
            isa = PBXProject;
            buildConfigurationList = #{ids[:project_config_list]} /* Build configuration list for PBXProject "DefdoSelfCare" */;
            compatibilityVersion = "Xcode 15.0";
            developmentRegion = en;
            hasScannedForEncodings = 0;
            knownRegions = (
                en,
                Base,
            );
            mainGroup = #{ids[:main_group]};
            packageReferences = (
                #{ids[:local_package_reference]} /* XCLocalSwiftPackageReference "." */,
            );
            productRefGroup = #{ids[:products_group]} /* Products */;
            projectDirPath = "";
            projectRoot = "";
            targets = (
                #{ids[:app_target]} /* DefdoSelfCare */,
            );
        };
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
        #{ids[:resources_phase]} /* Resources */ = {
            isa = PBXResourcesBuildPhase;
            buildActionMask = 2147483647;
            files = (
            );
            runOnlyForDeploymentPostprocessing = 0;
        };
/* End PBXResourcesBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
        #{ids[:sources_phase]} /* Sources */ = {
            isa = PBXSourcesBuildPhase;
            buildActionMask = 2147483647;
            files = (
                #{ids[:source_build_file]} /* DefdoSelfCareApp.swift in Sources */,
            );
            runOnlyForDeploymentPostprocessing = 0;
        };
/* End PBXSourcesBuildPhase section */

/* Begin XCLocalSwiftPackageReference section */
        #{ids[:local_package_reference]} /* XCLocalSwiftPackageReference "." */ = {
            isa = XCLocalSwiftPackageReference;
            relativePath = .;
        };
/* End XCLocalSwiftPackageReference section */

/* Begin XCSwiftPackageProductDependency section */
        #{ids[:kit_product_proxy]} /* DefdoSelfCareKit */ = {
            isa = XCSwiftPackageProductDependency;
            package = #{ids[:local_package_reference]} /* XCLocalSwiftPackageReference "." */;
            productName = DefdoSelfCareKit;
        };
/* End XCSwiftPackageProductDependency section */

/* Begin XCBuildConfiguration section */
        #{ids[:target_debug_config]} /* Debug */ = {
            isa = XCBuildConfiguration;
            buildSettings = {
                ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
                CODE_SIGN_ENTITLEMENTS = DefdoSelfCare/DefdoSelfCare.entitlements;
                CODE_SIGN_STYLE = Automatic;
                CURRENT_PROJECT_VERSION = 1;
                DEVELOPMENT_TEAM = "";
                GENERATE_INFOPLIST_FILE = NO;
                INFOPLIST_FILE = DefdoSelfCare/Info.plist;
                INFOPLIST_KEY_CFBundleDisplayName = "Defdo SelfCare";
                IPHONEOS_DEPLOYMENT_TARGET = 16.0;
                LD_RUNPATH_SEARCH_PATHS = (
                    "$(inherited)",
                    "@executable_path/Frameworks",
                );
                MARKETING_VERSION = 0.1.0;
                PRODUCT_BUNDLE_IDENTIFIER = dev.defdo.selfcare;
                PRODUCT_NAME = "$(TARGET_NAME)";
                SDKROOT = iphoneos;
                SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
                SUPPORTS_MACCATALYST = NO;
                SWIFT_EMIT_LOC_STRINGS = YES;
                SWIFT_VERSION = 6.0;
                TARGETED_DEVICE_FAMILY = "1,2";
            };
            name = Debug;
        };
        #{ids[:target_release_config]} /* Release */ = {
            isa = XCBuildConfiguration;
            buildSettings = {
                ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
                CODE_SIGN_ENTITLEMENTS = DefdoSelfCare/DefdoSelfCare.entitlements;
                CODE_SIGN_STYLE = Automatic;
                CURRENT_PROJECT_VERSION = 1;
                DEVELOPMENT_TEAM = "";
                GENERATE_INFOPLIST_FILE = NO;
                INFOPLIST_FILE = DefdoSelfCare/Info.plist;
                INFOPLIST_KEY_CFBundleDisplayName = "Defdo SelfCare";
                IPHONEOS_DEPLOYMENT_TARGET = 16.0;
                LD_RUNPATH_SEARCH_PATHS = (
                    "$(inherited)",
                    "@executable_path/Frameworks",
                );
                MARKETING_VERSION = 0.1.0;
                PRODUCT_BUNDLE_IDENTIFIER = dev.defdo.selfcare;
                PRODUCT_NAME = "$(TARGET_NAME)";
                SDKROOT = iphoneos;
                SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
                SUPPORTS_MACCATALYST = NO;
                SWIFT_EMIT_LOC_STRINGS = YES;
                SWIFT_VERSION = 6.0;
                TARGETED_DEVICE_FAMILY = "1,2";
            };
            name = Release;
        };
        #{ids[:project_debug_config]} /* Debug */ = {
            isa = XCBuildConfiguration;
            buildSettings = {
                ALWAYS_SEARCH_USER_PATHS = NO;
                ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOLS = YES;
                CLANG_ANALYZER_NONNULL = YES;
                CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
                CLANG_ENABLE_MODULES = YES;
                CLANG_ENABLE_OBJC_ARC = YES;
                CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
                CLANG_WARN_BOOL_CONVERSION = YES;
                CLANG_WARN_COMMA = YES;
                CLANG_WARN_CONSTANT_CONVERSION = YES;
                CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
                CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
                CLANG_WARN_DOCUMENT_COMMENTS = YES;
                CLANG_WARN_EMPTY_BODY = YES;
                CLANG_WARN_ENUM_CONVERSION = YES;
                CLANG_WARN_INFINITE_RECURSION = YES;
                CLANG_WARN_INT_CONVERSION = YES;
                CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
                CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
                CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
                CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
                CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;
                CLANG_WARN_STRICT_PROTOTYPES = YES;
                CLANG_WARN_SUSPICIOUS_MOVE = YES;
                CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
                CLANG_WARN_UNREACHABLE_CODE = YES;
                CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
                COPY_PHASE_STRIP = NO;
                DEBUG_INFORMATION_FORMAT = dwarf;
                ENABLE_STRICT_OBJC_MSGSEND = YES;
                ENABLE_TESTABILITY = YES;
                ENABLE_USER_SCRIPT_SANDBOXING = YES;
                GCC_C_LANGUAGE_STANDARD = gnu17;
                GCC_DYNAMIC_NO_PIC = NO;
                GCC_NO_COMMON_BLOCKS = YES;
                GCC_OPTIMIZATION_LEVEL = 0;
                GCC_PREPROCESSOR_DEFINITIONS = (
                    "DEBUG=1",
                    "$(inherited)",
                );
                GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
                GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
                GCC_WARN_UNDECLARED_SELECTOR = YES;
                GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
                GCC_WARN_UNUSED_FUNCTION = YES;
                GCC_WARN_UNUSED_VARIABLE = YES;
                IPHONEOS_DEPLOYMENT_TARGET = 16.0;
                LOCALIZATION_PREFERS_STRING_CATALOGS = YES;
                MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
                MTL_FAST_MATH = YES;
                ONLY_ACTIVE_ARCH = YES;
                SDKROOT = iphoneos;
                SWIFT_ACTIVE_COMPILATION_CONDITIONS = "DEBUG $(inherited)";
                SWIFT_OPTIMIZATION_LEVEL = "-Onone";
            };
            name = Debug;
        };
        #{ids[:project_release_config]} /* Release */ = {
            isa = XCBuildConfiguration;
            buildSettings = {
                ALWAYS_SEARCH_USER_PATHS = NO;
                ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOLS = YES;
                CLANG_ANALYZER_NONNULL = YES;
                CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
                CLANG_ENABLE_MODULES = YES;
                CLANG_ENABLE_OBJC_ARC = YES;
                CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
                CLANG_WARN_BOOL_CONVERSION = YES;
                CLANG_WARN_COMMA = YES;
                CLANG_WARN_CONSTANT_CONVERSION = YES;
                CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
                CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
                CLANG_WARN_DOCUMENT_COMMENTS = YES;
                CLANG_WARN_EMPTY_BODY = YES;
                CLANG_WARN_ENUM_CONVERSION = YES;
                CLANG_WARN_INFINITE_RECURSION = YES;
                CLANG_WARN_INT_CONVERSION = YES;
                CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
                CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
                CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
                CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
                CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;
                CLANG_WARN_STRICT_PROTOTYPES = YES;
                CLANG_WARN_SUSPICIOUS_MOVE = YES;
                CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
                CLANG_WARN_UNREACHABLE_CODE = YES;
                CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
                COPY_PHASE_STRIP = NO;
                DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
                ENABLE_NS_ASSERTIONS = NO;
                ENABLE_STRICT_OBJC_MSGSEND = YES;
                ENABLE_USER_SCRIPT_SANDBOXING = YES;
                GCC_C_LANGUAGE_STANDARD = gnu17;
                GCC_NO_COMMON_BLOCKS = YES;
                GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
                GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
                GCC_WARN_UNDECLARED_SELECTOR = YES;
                GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
                GCC_WARN_UNUSED_FUNCTION = YES;
                GCC_WARN_UNUSED_VARIABLE = YES;
                IPHONEOS_DEPLOYMENT_TARGET = 16.0;
                LOCALIZATION_PREFERS_STRING_CATALOGS = YES;
                MTL_ENABLE_DEBUG_INFO = NO;
                MTL_FAST_MATH = YES;
                SDKROOT = iphoneos;
                SWIFT_COMPILATION_MODE = wholemodule;
                VALIDATE_PRODUCT = YES;
            };
            name = Release;
        };
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
        #{ids[:project_config_list]} /* Build configuration list for PBXProject "DefdoSelfCare" */ = {
            isa = XCConfigurationList;
            buildConfigurations = (
                #{ids[:project_debug_config]} /* Debug */,
                #{ids[:project_release_config]} /* Release */,
            );
            defaultConfigurationIsVisible = 0;
            defaultConfigurationName = Release;
        };
        #{ids[:target_config_list]} /* Build configuration list for PBXNativeTarget "DefdoSelfCare" */ = {
            isa = XCConfigurationList;
            buildConfigurations = (
                #{ids[:target_debug_config]} /* Debug */,
                #{ids[:target_release_config]} /* Release */,
            );
            defaultConfigurationIsVisible = 0;
            defaultConfigurationName = Release;
        };
/* End XCConfigurationList section */
    };
    rootObject = #{ids[:project]} /* Project object */;
}
    """
  end

  defp contents_xcworkspacedata do
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <Workspace
       version = "1.0">
       <FileRef
          location = "self:">
       </FileRef>
    </Workspace>
    """
  end

  defp workspace_settings do
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
        <key>PreviewsEnabled</key>
        <false/>
    </dict>
    </plist>
    """
  end
end

XcodeprojGenerator.run(System.argv())
