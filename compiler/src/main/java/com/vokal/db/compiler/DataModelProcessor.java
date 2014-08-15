package com.vokal.db.compiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.vokal.db.annotations.GenerateHelpers")
public class DataModelProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        filer = env.getFiler();
    }

    private boolean nonStaticField(Element e) {
        Set<Modifier> mods = e.getModifiers();
        return e.getKind() == ElementKind.FIELD && 
            !mods.contains(Modifier.STATIC) && !mods.contains(Modifier.TRANSIENT);
    }

    private String getFqcn(Element element) {
        String p = elementUtils.getPackageOf(element).getQualifiedName().toString();
        log(p);
        return element.toString() + "Helpers";
    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> elements;
            elements = roundEnv.getElementsAnnotatedWith(com.vokal.db.annotations.GenerateHelpers.class);

            Iterator<? extends Element> iter = elements.iterator();
            while (iter.hasNext()) {
                Element element = iter.next();

                if (element.getKind() != ElementKind.CLASS) {
                    error("@DataModel must prefix a class -- "+element+
                            " is not a class");
                    continue;
                }

                try {
                    JavaFileObject jfo = filer.createSourceFile(getFqcn(element), element);

                    Writer writer = jfo.openWriter();
                    writer.write(writeColumns(element));
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    error(String.format("Unable to write injector for type %s: %s", element, e.getMessage()));
                }
            }
        }

        return true;
    }

    String getName(Element element) {
        String name = element.toString();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            if (i == 0 && name.charAt(0) == 'm' && name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
                b.append(name.charAt(++i));
            } else if (i == 0 || Character.isLowerCase(name.charAt(i))) {
                b.append(name.charAt(i));
            } else {
                b.append("_");
                b.append(name.charAt(i));
            }
        }
        return b.toString();
    }

    String writeColumns(Element e) {
        StringBuilder buffer = new StringBuilder("package ");
        buffer.append(elementUtils.getPackageOf(e).getQualifiedName().toString());
        buffer.append(";\n");
        buffer.append("import com.vokal.db.SQLiteTable;\n");
        buffer.append("import com.vokal.db.util.CursorGetter;\n");
        buffer.append("class ");
        buffer.append(e.getSimpleName());
        buffer.append("Helpers");
        buffer.append(" implements android.provider.BaseColumns {\n");
        List<? extends Element> subElements; 
        subElements = e.getEnclosedElements();
        Iterator<? extends Element> iterChild = subElements.iterator();
        StringBuilder statics = new StringBuilder();
        StringBuilder cols = new StringBuilder("aBuilder");
        StringBuilder creator = new StringBuilder();

        if (typeUtils.isSameType(((TypeElement) e).getSuperclass(), elementUtils.getTypeElement("com.vokal.db.AbstractDataModel").asType())) {
            cols.append(".addIntegerColumn(_ID).primaryKey()\n");
        }

        while (iterChild.hasNext()) {
            Element subElement = iterChild.next();
            if (nonStaticField(subElement)) {
                String name = getName(subElement);
                statics.append(String.format("public static final String %s = \"%s\";\n", name.toUpperCase(), name));

                if (isValidType(subElement)) {
                    cols.append(".add");
                    typeToString(subElement.asType(), cols, true);
                    cols.append("Column(");
                    cols.append(name.toUpperCase());
                    cols.append(")\n");

                    StringBuilder type = new StringBuilder();
                    typeToString(subElement.asType(), type, false);
                    creator.append(String.format(cursorLine, subElement.toString(), type.toString(), name.toUpperCase()));
                }
            }
        }

        cols.append(";");

        buffer.append(statics.toString());
        buffer.append("\n\n");
        buffer.append(String.format(builderClass, cols.toString()));

        buffer.append("\n\n");

        String n = e.getSimpleName().toString();

        buffer.append(String.format(cursorClass, n, n, n, n, creator.toString()));

        buffer.append("}");
        return buffer.toString();
    }

    public static final String builderClass = new StringBuilder("public static class TableCreator implements SQLiteTable.TableCreator {\n")
        .append("@Override\npublic SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder) {\n%s\nreturn aBuilder.build();\n}\n")
        .append("@Override\npublic SQLiteTable updateTableSchema(SQLiteTable.Updater aUpdater, int aOldVersion) {\nreturn null;\n}\n};\n")
        .toString();

    public static final String cursorClass = new StringBuilder("public static class CursorCreator implements com.vokal.db.util.CursorCreator<%s> {\n")
        .append("@Override\npublic %s createFromCursorGetter(CursorGetter getter) {\n%s result = new %s();\n%s\nreturn result;\n}\n};\n")
        .toString();

    public static final String cursorLine = "result.%s = getter.get%s(%s);\n";

    void log(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    /**
     * Appends a string for {@code type} to {@code result}. Primitive types are
     * always boxed.
     *
     * @param innerClassSeparator either '.' or '$', which will appear in a
     *     class name like "java.lang.Map.Entry" or "java.lang.Map$Entry".
     *     Use '.' for references to existing types in code. Use '$' to define new
     *     class names and for strings that will be used by runtime reflection.
     */
    public  void typeToString(final TypeMirror type, final StringBuilder result,
            final boolean simple) {
        final char innerClassSeparator = '$';
        type.accept(new SimpleTypeVisitor6<Void, Void>() {
            @Override public Void visitDeclared(DeclaredType declaredType, Void v) {
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                rawTypeToString(result, typeElement, innerClassSeparator);
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (!typeArguments.isEmpty()) {
                    result.append("<");
                    for (int i = 0; i < typeArguments.size(); i++) {
                        if (i != 0) {
                            result.append(", ");
                        }
                        typeToString(typeArguments.get(i), result, simple);
                    }
                    result.append(">");
                }
                return null;
            }
            @Override public Void visitPrimitive(PrimitiveType primitiveType, Void v) {
                if (simple) {
                    result.append(simplebox((PrimitiveType) type).getSimpleName());
                } else {
                    String t = box((PrimitiveType) type).getSimpleName().toString();
                    if ("Integer".equals(t)) {
                        result.append("Int");
                    } else {
                        result.append(t);
                    }
                }
                return null;
            }
            @Override public Void visitArray(ArrayType arrayType, Void v) {
                TypeMirror type = arrayType.getComponentType();
                if (type instanceof PrimitiveType) {
                    result.append(type.toString()); // Don't box, since this is an array.
                } else {
                    typeToString(arrayType.getComponentType(), result, simple);
                }
                result.append("[]");
                return null;
            }
            @Override public Void visitTypeVariable(TypeVariable typeVariable, Void v) {
                result.append(typeVariable.asElement().getSimpleName());
                return null;
            }
            @Override public Void visitError(ErrorType errorType, Void v) {
                // Paramterized types which don't exist are returned as an error type whose name is "<any>"
                if ("<any>".equals(errorType.toString())) {
                    throw new UnsupportedOperationException(
                            "Type reported as <any> is likely a not-yet generated parameterized type.");
                }
                // TODO(cgruber): Figure out a strategy for non-FQCN cases.
                result.append(errorType.toString());
                return null;
            }
            @Override protected Void defaultAction(TypeMirror typeMirror, Void v) {
                throw new UnsupportedOperationException(
                        "Unexpected TypeKind " + typeMirror.getKind() + " for "  + typeMirror);
            }
        }, null);
    }

     void rawTypeToString(StringBuilder result, TypeElement type,
            char innerClassSeparator) {
        String qualifiedName = type.getSimpleName().toString();
        result.append(qualifiedName.replace('.', innerClassSeparator));
    }

    public static final Set<String> VALID_TYPES = new LinkedHashSet<String>() {{
        add("Byte");
        add("Short");
        add("Integer");
        add("Long");
        add("Float");
        add("Double");
        add("Boolean");
        add("Character");
        add("String");
    }};

    private boolean isValidType(Element element) {
        boolean isClass = element.asType() instanceof DeclaredType;
        log("Is class: " + element.toString() + " - " + isClass);
        return element.asType().getKind().isPrimitive() || 
            (isClass && VALID_TYPES.contains(((DeclaredType) element.asType()).asElement().getSimpleName().toString()));
    }

    private  Class<?> simplebox(PrimitiveType primitiveType) {
        switch (primitiveType.getKind()) {
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
                return Integer.class;
            case CHAR:
                return String.class;
            default:
                throw new AssertionError();
        }
    }

    private  Class<?> box(PrimitiveType primitiveType) {
        switch (primitiveType.getKind()) {
            case BYTE:
                return Byte.class;
            case SHORT:
                return Short.class;
            case INT:
                return Integer.class;
            case LONG:
                return Long.class;
            case FLOAT:
                return Float.class;
            case DOUBLE:
                return Double.class;
            case BOOLEAN:
                return Boolean.class;
            case CHAR:
                return Character.class;
            default:
                throw new AssertionError();
        }
    }
}
