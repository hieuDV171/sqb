package com.frozenheart.backend.core.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.TemporalAccessor;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiDocumentationTest {
    private final Set<Class<?>> models = new TreeSet<>(Comparator.comparing(Class::getName));
    private final List<String> missing = new ArrayList<>();
    private int operations;

    @Test void applicationOperationsAndReachableModelsHaveDescriptionsAndExamples() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (var bean : scanner.findCandidateComponents("com.frozenheart.backend")) {
            Class<?> controller = Class.forName(bean.getBeanClassName());
            if (controller.isAnnotationPresent(Hidden.class)) continue;
            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null
                        || method.isAnnotationPresent(Hidden.class)) continue;
                operations++;
                String name = controller.getSimpleName() + "#" + method.getName();
                Operation operation = method.getAnnotation(Operation.class);
                if (operation == null || operation.summary().isBlank() || operation.description().isBlank()) {
                    missing.add("OPERATION\t" + name);
                }
                visit(method.getGenericReturnType());
                for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                    Parameter doc = parameter.getAnnotation(Parameter.class);
                    if (doc != null && doc.hidden()) continue;
                    boolean wireParameter = parameter.isAnnotationPresent(RequestParam.class)
                            || parameter.isAnnotationPresent(PathVariable.class)
                            || parameter.isAnnotationPresent(RequestPart.class);
                    if (wireParameter && (doc == null || doc.description().isBlank()
                            || (needsExample(parameter.getParameterizedType()) && doc.example().isBlank()
                            && doc.schema().example().isBlank() && doc.examples().length == 0))) {
                        missing.add("PARAMETER\t" + name + "\t" + parameter.getName());
                    }
                    visit(parameter.getParameterizedType());
                }
            }
        }
        Map<String, Class<?>> schemaNames = new HashMap<>();
        for (Class<?> model : models) {
            Schema modelDoc = model.getAnnotation(Schema.class);
            String schemaName = modelDoc == null || modelDoc.name().isBlank() ? model.getSimpleName() : modelDoc.name();
            Class<?> previous = schemaNames.putIfAbsent(schemaName, model);
            if (previous != null) missing.add("MODEL_NAME_COLLISION\t" + schemaName + "\t" + previous.getName() + "\t" + model.getName());
            if (modelDoc == null || modelDoc.description().isBlank()) missing.add("MODEL\t" + model.getName());
            for (Field field : model.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic() || field.isAnnotationPresent(JsonIgnore.class)) continue;
                Schema doc = field.getAnnotation(Schema.class);
                if (doc != null && doc.hidden()) continue;
                boolean description = doc == null || doc.description().isBlank();
                boolean example = needsExample(field.getGenericType()) && (doc == null || doc.example().isBlank());
                if (description || example) missing.add("FIELD\t" + model.getName() + "#" + field.getName()
                        + "\t" + field.getGenericType().getTypeName() + "\tdescription=" + description + "\texample=" + example);
                if (doc != null && !doc.example().isBlank()) checkExample(field, doc.example());
            }
        }
        Collections.sort(missing);
        Path report = Path.of("build/reports/swagger-audit.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "Operations=" + operations + ", models=" + models.size() + ", gaps=" + missing.size()
                + "\n" + String.join("\n", missing));
        assertTrue(missing.isEmpty(), () -> "Swagger documentation gaps (see " + report + "):\n"
                + String.join("\n", missing.stream().limit(35).toList()));
    }

    @Test void generatedSchemasPreserveSnakeCaseAndPolymorphicCriteria() throws Exception {
        var converters = new ModelConverters();
        converters.addConverter(new OpenApiConfig(new com.frozenheart.backend.core.security.AuthCompatibilityPolicy(false)).modelResolver());
        var upload = resolve(converters, com.frozenheart.backend.modules.media.dto.LectureVideoUpload.StartRequest.class);
        assertTrue(upload.getProperties().keySet().containsAll(List.of("file_name", "content_type", "file_size")));
        assertNotNull(upload.getProperties().get("file_size").getExample());
        var post = resolve(converters, com.frozenheart.backend.modules.post.dto.PostResponseDto.class);
        assertTrue(post.getProperties().keySet().containsAll(List.of("post_id", "media_urls", "created_at", "my_reaction")));
        var media = resolve(converters, com.frozenheart.backend.core.entity.media.MediaItem.class);
        assertTrue(media.getProperties().containsKey("thumbnail_url"));
        var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(com.frozenheart.backend.modules.badge.dto.CreateBadgeRequestDto.class));
        assertTrue(resolved.referencedSchemas.keySet().containsAll(List.of("CounterThresholdCriteria", "StreakCriteria",
                "SubjectMasteryCriteria", "LeaderboardRankCriteria", "ManualGrantCriteria")));
        var criteria = resolved.referencedSchemas.get("BadgeCriteria");
        assertNotNull(criteria);
        assertEquals("type", criteria.getDiscriminator().getPropertyName());
        assertEquals(5, criteria.getOneOf().size());

        // Boolean fields prefixed with "is" must match the actual Jackson wire names too.
        var runtimeMapper = tools.jackson.databind.json.JsonMapper.builder()
                .propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE).build();
        for (Object sample : List.of(
                com.frozenheart.backend.modules.block.dto.BlockResponseDto.builder().blockedUserId(7L).isBlocked(true).build(),
                com.frozenheart.backend.modules.conversation.dto.MessageDto.builder().messageId(42L).isEdited(false).build())) {
            var payload = new ObjectMapper().readTree(runtimeMapper.writeValueAsString(sample));
            Set<String> wireNames = new HashSet<>();
            payload.fieldNames().forEachRemaining(wireNames::add);
            assertEquals(wireNames, resolve(converters, sample.getClass()).getProperties().keySet());
        }
    }

    private io.swagger.v3.oas.models.media.Schema<?> resolve(ModelConverters converters, Type type) {
        var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(type));
        var schema = resolved.schema;
        if (schema.get$ref() != null) schema = resolved.referencedSchemas.get(schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1));
        assertNotNull(schema);
        assertNotNull(schema.getProperties());
        return schema;
    }

    private void checkExample(Field field, String value) {
        Class<?> type = field.getType();
        try {
            if (type.isEnum()) {
                boolean matches = Arrays.stream(type.getEnumConstants()).anyMatch(item -> ((Enum<?>) item).name().equals(value));
                if (!matches) throw new IllegalArgumentException("not an enum value");
            } else if (type == boolean.class || type == Boolean.class) {
                if (!Set.of("true", "false").contains(value)) throw new IllegalArgumentException("not boolean");
            } else if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != char.class)) {
                new BigDecimal(value);
            } else if (type == UUID.class) UUID.fromString(value);
            else if (type == Instant.class) Instant.parse(value);
            else if (type == LocalDate.class) LocalDate.parse(value);
            else if (Collection.class.isAssignableFrom(type)) {
                if (!new ObjectMapper().readTree(value).isArray()) throw new IllegalArgumentException("not JSON array");
            }
        } catch (Exception invalid) {
            missing.add("INVALID_EXAMPLE\t" + field.getDeclaringClass().getName() + "#" + field.getName() + "\t" + value);
        }
    }

    private void visit(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            visit(parameterized.getRawType());
            for (Type argument : parameterized.getActualTypeArguments()) visit(argument);
        } else if (type instanceof Class<?> model) {
            if (model.isArray()) { visit(model.getComponentType()); return; }
            if (model.isEnum() || !model.getName().startsWith("com.frozenheart.backend.")) return;
            if (model.isAnnotationPresent(jakarta.persistence.Entity.class)) return;
            if (!models.add(model)) return;
            var subtypes = model.getAnnotation(com.fasterxml.jackson.annotation.JsonSubTypes.class);
            if (subtypes != null) for (var subtype : subtypes.value()) visit(subtype.value());
            for (Field field : model.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isAnnotationPresent(JsonIgnore.class)) visit(field.getGenericType());
            }
            if (model.getGenericSuperclass() != null) visit(model.getGenericSuperclass());
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) visit(bound);
        }
    }

    private boolean needsExample(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            return parameterized.getRawType() instanceof Class<?> raw && Collection.class.isAssignableFrom(raw)
                    && Arrays.stream(parameterized.getActualTypeArguments()).allMatch(this::needsExample);
        }
        return type instanceof Class<?> model && (model.isPrimitive() || model == String.class || model == Boolean.class
                || Number.class.isAssignableFrom(model) || TemporalAccessor.class.isAssignableFrom(model) || model == UUID.class);
    }
}
