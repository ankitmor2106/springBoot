package com.example.employeemanager.view;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.LinkedHashSet;
import java.util.Set;

public class FormBindingDialect extends StandardDialect {
    private static final String PREFIX = "th";
    private static final String SELECTED_VALUE = "formBindingSelectedValue";
    private static final int PRECEDENCE = 1000;

    public FormBindingDialect() {
        super("Standalone form binding", PREFIX, 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        Set<IProcessor> processors = new LinkedHashSet<>(super.getProcessors(dialectPrefix));
        processors.add(new FieldProcessor(dialectPrefix, "input"));
        processors.add(new FieldProcessor(dialectPrefix, "select"));
        processors.add(new OptionProcessor(dialectPrefix));
        return processors;
    }

    private static Object evaluate(ITemplateContext context, String expressionText) {
        IStandardExpressionParser parser =
                StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, expressionText);
        return expression.execute(context);
    }

    private static final class FieldProcessor extends AbstractAttributeTagProcessor {
        private FieldProcessor(String prefix, String elementName) {
            super(TemplateMode.HTML, prefix, elementName, false, "field", true, PRECEDENCE, true);
        }

        @Override
        protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
                                 String attributeValue, IElementTagStructureHandler structureHandler) {
            if (!context.hasSelectionTarget()) {
                throw new TemplateProcessingException("th:field must be inside a form with th:object.");
            }
            String fieldName = propertyName(attributeValue);
            Object value = evaluate(context, attributeValue);
            structureHandler.setAttribute("name", fieldName);
            structureHandler.setAttribute("id", tag.hasAttribute("id") ? tag.getAttributeValue("id") : fieldName);
            if ("select".equalsIgnoreCase(tag.getElementCompleteName())) {
                structureHandler.setLocalVariable(SELECTED_VALUE, value);
            } else if (!"password".equalsIgnoreCase(tag.getAttributeValue("type"))) {
                structureHandler.setAttribute("value", value == null ? "" : value.toString());
            }
        }

        private static String propertyName(String expression) {
            String trimmed = expression.trim();
            if (!trimmed.startsWith("*{") || !trimmed.endsWith("}")) {
                throw new TemplateProcessingException("th:field requires a selection expression such as *{name}.");
            }
            return trimmed.substring(2, trimmed.length() - 1).trim();
        }
    }

    private static final class OptionProcessor extends AbstractElementTagProcessor {
        private OptionProcessor(String prefix) {
            super(TemplateMode.HTML, prefix, "option", false, null, false, PRECEDENCE + 1);
        }

        @Override
        protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
                                 IElementTagStructureHandler structureHandler) {
            Object selectedValue = context.getVariable(SELECTED_VALUE);
            if (selectedValue == null) {
                return;
            }
            Object optionValue;
            if (tag.hasAttribute("th:value")) {
                optionValue = evaluate(context, tag.getAttributeValue("th:value"));
            } else {
                optionValue = tag.getAttributeValue("value");
            }
            if (matches(selectedValue, optionValue)) {
                structureHandler.setAttribute("selected", "selected");
            } else {
                structureHandler.removeAttribute("selected");
            }
        }

        private static boolean matches(Object selected, Object option) {
            if (selected instanceof Iterable<?> values) {
                for (Object value : values) {
                    if (value != null && value.toString().equals(option == null ? "" : option.toString())) {
                        return true;
                    }
                }
                return false;
            }
            return selected.toString().equals(option == null ? "" : option.toString());
        }
    }
}
