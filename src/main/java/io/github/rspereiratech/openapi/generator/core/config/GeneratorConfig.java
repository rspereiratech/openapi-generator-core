/*
 *   ___                   _   ___ ___
 *  / _ \ _ __  ___ _ _   /_\ | _ \_ _|
 * | (_) | '_ \/ -_) ' \ / _ \|  _/| |
 *  \___/| .__/\___|_||_/_/ \_\_| |___|   Generator
 *       |_|
 *
 * MIT License - Copyright (c) 2026 Rui Pereira
 * See LICENSE in the project root for full license information.
 */
package io.github.rspereiratech.openapi.generator.core.config;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable configuration object for the OpenAPI generator.
 *
 * <p>Instances must be created using the {@link Builder}, which provides
 * a fluent API for configuring base packages, servers, controller stereotypes,
 * and metadata such as title, version, contact, and license information.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * GeneratorConfig config = GeneratorConfig.builder()
 *     .basePackage("com.example.myapp")
 *     .title("My API")
 *     .version("1.0.0")
 *     .server("https://api.example.com", "Production")
 *     .build();
 * }</pre>
 *
 * <p>This record is immutable. All collection-based properties are defensively
 * copied during construction and are guaranteed to be non-null.</p>
 *
 * @author ruispereira
 */
public record GeneratorConfig(
        /* Base packages to scan for Spring MVC controllers. Must not be null or empty. */
        List<String> basePackages,

        /* Output file path where the generated OpenAPI specification will be written. */
        String outputFile,

        /* API title written into the {@code info.title} section. */
        String title,

        /* API description written into the {@code info.description} section. */
        String description,

        /* API version written into the {@code info.version} section. */
        String version,

        /* Ordered list of servers to include in the {@code servers} section. Never null, may be empty. */
        List<ServerConfig> servers,

        /*
         * Fully-qualified annotation names treated as controller stereotypes
         * in addition to {@code @RestController} and {@code @Controller}.
         * Never null, may be empty.
         */
        List<String> controllerAnnotations,

        /* Contact name written into {@code info.contact.name}. May be null. */
        String contactName,

        /* Contact email written into {@code info.contact.email}. May be null. */
        String contactEmail,

        /* Contact URL written into {@code info.contact.url}. May be null. */
        String contactUrl,

        /* License name written into {@code info.license.name}. May be null. */
        String licenseName,

        /* License URL written into {@code info.license.url}. May be null. */
        String licenseUrl,

        /* Whether the generated OpenAPI YAML should be pretty-printed. */
        boolean prettyPrint,

        /* Output format for the generated specification: YAML (default) or JSON. */
        OutputFormat outputFormat,

        /* Security schemes to register under components/securitySchemes. Never null, may be empty. */
        List<SecuritySchemeConfig> securitySchemes,

        /*
         * Optional application name appended to every server URL as a path segment.
         * When set, each server URL is suffixed with {@code /<contextPath>/}.
         * For example, {@code https://api.example.com} becomes
         * {@code https://api.example.com/vcc-superx-api/}. May be null.
         */
        String contextPath,

        /*
         * When {@code true}, controllers are sorted alphabetically by canonical
         * class name before processing, and the resulting paths are sorted
         * alphabetically in the final spec, guaranteeing a deterministic output
         * regardless of filesystem or JVM ordering. Defaults to {@code false}.
         */
        boolean sortOutput,

        /*
         * When {@code true} (default), the processor skips the built-in set of
         * framework-injected parameter types (e.g. {@code Locale},
         * {@code HttpServletRequest}, {@code Principal}) that must never appear
         * as OpenAPI parameters.
         */
        boolean ignoreDefaultParamTypes,

        /*
         * Additional fully-qualified class names of parameter types to ignore,
         * on top of the built-in defaults. Never null, may be empty.
         */
        List<String> additionalIgnoredParamTypes
) {
    /**
     * Compact constructor — enforces immutability and validates required fields.
     *
     * @throws NullPointerException     if {@code basePackages}, {@code outputFile}, {@code title},
     *                                  {@code version}, or {@code outputFormat} is {@code null}
     * @throws IllegalArgumentException if {@code basePackages} is empty, any package entry is blank,
     *                                  {@code outputFile}, {@code title}, or {@code version} is blank,
     *                                  or any {@code controllerAnnotations} entry is blank
     */
    public GeneratorConfig {
        Preconditions.checkNotNull(basePackages, "basePackages must not be null");
        Preconditions.checkArgument(!basePackages.isEmpty(), "at least one basePackage must be specified");
        basePackages.forEach(pkg -> Preconditions.checkArgument(
                pkg != null && !pkg.isBlank(), "each basePackage must not be null or blank"));
        basePackages = List.copyOf(basePackages);

        Preconditions.checkNotNull(outputFile, "outputFile must not be null");
        Preconditions.checkArgument(!outputFile.isBlank(), "outputFile must not be blank");

        Preconditions.checkNotNull(title, "title must not be null");
        Preconditions.checkArgument(!title.isBlank(), "title must not be blank");

        Preconditions.checkNotNull(version, "version must not be null");
        Preconditions.checkArgument(!version.isBlank(), "version must not be blank");

        Preconditions.checkNotNull(outputFormat, "outputFormat must not be null");

        servers = List.copyOf(servers != null ? servers : List.of());

        securitySchemes = List.copyOf(securitySchemes != null ? securitySchemes : List.of());

        List<String> resolvedAnnotations = controllerAnnotations != null ? controllerAnnotations : List.of();
        resolvedAnnotations.forEach(fqn -> Preconditions.checkArgument(
                fqn != null && !fqn.isBlank(), "each controllerAnnotation must not be null or blank"));
        controllerAnnotations = List.copyOf(resolvedAnnotations);

        additionalIgnoredParamTypes = List.copyOf(additionalIgnoredParamTypes != null ? additionalIgnoredParamTypes : List.of());
    }

    /**
     * Creates a new {@link Builder} instance for constructing
     * an immutable {@link GeneratorConfig}.
     *
     * <p>Use the returned builder to set configuration properties
     * before calling {@link Builder#build()}.</p>
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for creating immutable {@link GeneratorConfig} instances.
     *
     * <p>The builder supports incremental configuration and allows multiple
     * base packages, servers, and additional controller annotations to be added.
     * All collections are accumulated internally and validated when
     * {@link #build()} is invoked.</p>
     *
     * <p>At least one base package must be provided before calling {@link #build()}.</p>
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {
        private final List<String>               basePackages               = new ArrayList<>();
        private final List<ServerConfig>         servers                    = new ArrayList<>();
        private final List<String>               controllerAnnotations      = new ArrayList<>();
        private final List<SecuritySchemeConfig> securitySchemes            = new ArrayList<>();
        private final List<String>               additionalIgnoredParamTypes = new ArrayList<>();
        private String  outputFile  = "docs/swagger/openapi.yaml";
        private String  title       = "API";
        private String  description = "";
        private String  version     = "1.0.0";
        private String  contactName;
        private String  contactEmail;
        private String  contactUrl;
        private String  licenseName;
        private String  licenseUrl;
        private boolean prettyPrint              = true;
        private boolean sortOutput               = false;
        private boolean ignoreDefaultParamTypes  = true;
        private OutputFormat outputFormat = OutputFormat.YAML;
        private String  contextPath;

        /**
         * Adds a single base package to scan for Spring MVC controllers.
         *
         * @param pkg the fully-qualified package name; must not be {@code null}
         * @return this builder
         * @throws NullPointerException     if {@code pkg} is {@code null}
         * @throws IllegalArgumentException if {@code pkg} is blank
         */
        public Builder basePackage(String pkg) {
            Preconditions.checkNotNull(pkg, "basePackage must not be null");
            Preconditions.checkArgument(!pkg.isBlank(), "basePackage must not be blank");
            this.basePackages.add(pkg);
            return this;
        }

        /**
         * Adds all packages from the supplied list to the set of packages to scan.
         *
         * @param packages the list of fully-qualified package names; must not be {@code null}
         * @return this builder
         * @throws NullPointerException     if {@code packages} is {@code null}
         * @throws IllegalArgumentException if {@code packages} is empty
         */
        public Builder basePackages(List<String> packages) {
            Preconditions.checkNotNull(packages, "basePackages must not be null");
            Preconditions.checkArgument(!packages.isEmpty(), "basePackages list must not be empty");
            this.basePackages.addAll(packages);
            return this;
        }

        /** Adds a fully configured {@link ServerConfig}. */
        public Builder server(ServerConfig server) {
            Preconditions.checkNotNull(server, "server must not be null");
            this.servers.add(server);
            return this;
        }

        /** Convenience: adds a server with only a URL. */
        public Builder server(String url) {
            return server(ServerConfig.of(url));
        }

        /** Convenience: adds a server with URL and description. */
        public Builder server(String url, String description) {
            return server(ServerConfig.of(url, description));
        }

        /** Replaces the entire server list. */
        public Builder servers(List<ServerConfig> servers) {
            Preconditions.checkNotNull(servers, "servers must not be null");
            Preconditions.checkArgument(!servers.isEmpty(), "servers list must not be empty");
            this.servers.clear();
            this.servers.addAll(servers);
            return this;
        }

        /**
         * Backward-compat shorthand: adds a server with the given URL and no description.
         * Equivalent to {@code server(url)}.
         */
        public Builder serverUrl(String url) {
            return server(url);
        }

        /** Adds a single annotation FQN to treat as a controller stereotype. */
        public Builder controllerAnnotation(String fqn) {
            Preconditions.checkNotNull(fqn, "'controllerAnnotation' must not be null");
            Preconditions.checkArgument(!fqn.isBlank(), "'controllerAnnotation' must not be blank");
            this.controllerAnnotations.add(fqn);
            return this;
        }

        /** Adds a single {@link SecuritySchemeConfig} to the security schemes list. */
        public Builder securityScheme(SecuritySchemeConfig scheme) {
            Preconditions.checkNotNull(scheme, "securityScheme must not be null");
            this.securitySchemes.add(scheme);
            return this;
        }

        /** Replaces the entire security schemes list. */
        public Builder securitySchemes(List<SecuritySchemeConfig> schemes) {
            Preconditions.checkNotNull(schemes, "securitySchemes must not be null");
            this.securitySchemes.clear();
            this.securitySchemes.addAll(schemes);
            return this;
        }

        /** Replaces the entire list of additional controller annotation FQNs. */
        public Builder controllerAnnotations(List<String> fqns) {
            Preconditions.checkNotNull(fqns, "'controllerAnnotations' must not be null");
            Preconditions.checkArgument(!fqns.isEmpty(), "'controllerAnnotations' must not be empty");
            this.controllerAnnotations.clear();
            this.controllerAnnotations.addAll(fqns);
            return this;
        }

        /** Sets the path of the generated output file. @return this builder */
        public Builder outputFile(String outputFile) {
            Preconditions.checkNotNull(outputFile, "outputFile must not be null");
            Preconditions.checkArgument(!outputFile.isBlank(), "outputFile must not be blank");
            this.outputFile = outputFile;
            return this;
        }

        /** Sets the API title written into the {@code info} block. @return this builder */
        public Builder title(String title) {
            Preconditions.checkNotNull(title, "title must not be null");
            Preconditions.checkArgument(!title.isBlank(), "title must not be blank");
            this.title = title;
            return this;
        }

        /** Sets the API description written into the {@code info} block. @return this builder */
        public Builder description(String description)   { this.description  = description;  return this; }

        /** Sets the API version written into the {@code info} block. @return this builder */
        public Builder version(String version) {
            Preconditions.checkNotNull(version, "version must not be null");
            Preconditions.checkArgument(!version.isBlank(), "version must not be blank");
            this.version = version;
            return this;
        }
        /** Sets the contact name for the {@code info.contact} block. @return this builder */
        public Builder contactName(String contactName)   { this.contactName  = contactName;  return this; }
        /** Sets the contact e-mail for the {@code info.contact} block. @return this builder */
        public Builder contactEmail(String contactEmail) { this.contactEmail = contactEmail; return this; }
        /** Sets the contact URL for the {@code info.contact} block. @return this builder */
        public Builder contactUrl(String contactUrl)     { this.contactUrl   = contactUrl;   return this; }
        /** Sets the license name for the {@code info.license} block. @return this builder */
        public Builder licenseName(String licenseName)   { this.licenseName  = licenseName;  return this; }
        /** Sets the license URL for the {@code info.license} block. @return this builder */
        public Builder licenseUrl(String licenseUrl)     { this.licenseUrl   = licenseUrl;   return this; }
        /** Sets whether the output should be pretty-printed. @return this builder */
        public Builder prettyPrint(boolean prettyPrint)  { this.prettyPrint  = prettyPrint;  return this; }

        /**
         * Controls whether controllers and paths are sorted alphabetically.
         * When {@code true} (default), controllers are sorted by canonical class name
         * before processing and paths are sorted alphabetically in the final spec,
         * guaranteeing deterministic output across machines and builds.
         *
         * @param sortOutput {@code false} to preserve discovery order
         * @return this builder
         */
        public Builder sortOutput(boolean sortOutput)    { this.sortOutput   = sortOutput;   return this; }

        /**
         * Sets the optional application name that is appended to every server URL
         * as a path segment with a trailing slash.
         * For example, {@code contextPath("vcc-superx-api")} turns
         * {@code https://api.example.com} into {@code https://api.example.com/vcc-superx-api/}.
         *
         * @param contextPath the application context path; may be {@code null} to disable
         * @return this builder
         */
        public Builder contextPath(String contextPath) { this.contextPath = contextPath; return this; }

        /**
         * Controls whether the built-in set of framework-injected parameter types
         * (e.g. {@code Locale}, {@code HttpServletRequest}, {@code Principal}) is skipped
         * during parameter processing. Defaults to {@code true}.
         *
         * @param ignoreDefaultParamTypes {@code false} to disable the built-in ignore list
         * @return this builder
         */
        public Builder ignoreDefaultParamTypes(boolean ignoreDefaultParamTypes) {
            this.ignoreDefaultParamTypes = ignoreDefaultParamTypes;
            return this;
        }

        /**
         * Adds a single fully-qualified class name to the additional ignored parameter types list.
         *
         * @param fqn the fully-qualified class name to ignore; must not be {@code null} or blank
         * @return this builder
         */
        public Builder additionalIgnoredParamType(String fqn) {
            Preconditions.checkNotNull(fqn, "additionalIgnoredParamType must not be null");
            Preconditions.checkArgument(!fqn.isBlank(), "additionalIgnoredParamType must not be blank");
            this.additionalIgnoredParamTypes.add(fqn);
            return this;
        }

        /**
         * Replaces the entire list of additional ignored parameter type FQNs.
         *
         * @param fqns the list of fully-qualified class names; must not be {@code null}
         * @return this builder
         */
        public Builder additionalIgnoredParamTypes(List<String> fqns) {
            Preconditions.checkNotNull(fqns, "additionalIgnoredParamTypes must not be null");
            this.additionalIgnoredParamTypes.clear();
            this.additionalIgnoredParamTypes.addAll(fqns);
            return this;
        }

        /**
         * Sets the output format ({@link OutputFormat#YAML} or {@link OutputFormat#JSON}).
         * Defaults to {@link OutputFormat#YAML} when not specified.
         *
         * @param outputFormat the desired output format; must not be {@code null}
         * @return this builder
         * @throws NullPointerException if {@code outputFormat} is {@code null}
         */
        public Builder outputFormat(OutputFormat outputFormat) {
            Preconditions.checkNotNull(outputFormat, "outputFormat must not be null");
            this.outputFormat = outputFormat;
            return this;
        }

        /**
         * Builds and returns an immutable {@link GeneratorConfig} from the accumulated state.
         *
         * @return the constructed {@link GeneratorConfig}
         * @throws IllegalArgumentException if no base package has been specified or any required field is invalid
         */
        public GeneratorConfig build() {
            return new GeneratorConfig(
                    basePackages, outputFile, title, description, version,
                    servers, controllerAnnotations,
                    contactName, contactEmail, contactUrl,
                    licenseName, licenseUrl, prettyPrint, outputFormat,
                    securitySchemes, contextPath, sortOutput,
                    ignoreDefaultParamTypes, additionalIgnoredParamTypes);
        }
    }
}
