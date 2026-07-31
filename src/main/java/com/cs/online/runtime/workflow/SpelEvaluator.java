package com.cs.online.runtime.workflow;

import com.cs.online.context.Context;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用 SpEL 对 Context 变量求值，供 IF 节点判断走哪条分支。
 * 表达式里通过 #variables['name'] 访问 Context.variables() 里的值。
 */
@Component
public class SpelEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();

    public Object evaluate(String expression, Context context) {
        Map<String, Object> variableValues = new LinkedHashMap<>();
        context.variables().all().forEach((name, variable) -> variableValues.put(name, variable.getValue()));

        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("variables", variableValues);

        Expression parsed = parser.parseExpression(expression);
        return parsed.getValue(evalContext);
    }
}
