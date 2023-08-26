package de.jplag.antlr;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

import org.antlr.v4.runtime.Token;

import de.jplag.TokenType;
import de.jplag.semantics.CodeSemantics;
import de.jplag.semantics.VariableRegistry;

/**
 * The abstract visitor.
 * @param <T> The type of the visited entity.
 */
public abstract class AbstractVisitor<T> {
    private final Predicate<T> condition;
    private final List<Consumer<HandlerData<T>>> entryHandlers;
    private Function<T, CodeSemantics> semanticsSupplier;

    /**
     * @param condition The condition for the visit.
     */
    AbstractVisitor(Predicate<T> condition) {
        this.condition = condition;
        this.entryHandlers = new ArrayList<>();
    }

    /**
     * Add an action the visitor runs upon entering the entity.
     * @param handler The action, takes the entity and the variable registry as parameter.
     * @return Self
     */
    public AbstractVisitor<T> onEnter(BiConsumer<T, VariableRegistry> handler) {
        entryHandlers.add(handlerData -> handler.accept(handlerData.entity(), handlerData.variableRegistry()));
        return this;
    }

    /**
     * Add an action the visitor runs upon entering the entity.
     * @param handler The action, takes the entity as parameter.
     * @return Self
     */
    public AbstractVisitor<T> onEnter(Consumer<T> handler) {
        entryHandlers.add(handlerData -> handler.accept(handlerData.entity()));
        return this;
    }

    /**
     * Tell the visitor that it should generate a token upon entering the entity. Should only be invoked once per visitor.
     * @param tokenType The type of the token.
     * @return Self
     */
    public AbstractVisitor<T> mapEnter(TokenType tokenType) {
        map(entryHandlers, tokenType, this::extractEnterToken);
        return this;
    }

    /**
     * Tell the visitor that it should generate a token upon entering the entity. Should only be invoked once per visitor.
     * Alias for {@link #mapEnter(TokenType)}.
     * @param tokenType The type of the token.
     * @return Self
     */
    public AbstractVisitor<T> map(TokenType tokenType) {
        mapEnter(tokenType);
        return this;
    }

    /**
     * Tell the visitor that if it generates a token upon entering the entity, it should have semantics. If it doesn't
     * generate a token, the semantics are discarded. This is not checked and does not lead to a warning.
     * @param semanticsSupplier A function that takes the entity and returns the semantics.
     * @return Self
     */
    public AbstractVisitor<T> withSemantics(Function<T, CodeSemantics> semanticsSupplier) {
        this.semanticsSupplier = semanticsSupplier;
        return this;
    }

    /**
     * Tell the visitor that if it generates a token upon entering the entity, it should have semantics. If it doesn't
     * generate a token, the semantics are discarded. This is not checked and does not lead to a warning.
     * @param semanticsSupplier A function that returns the semantics.
     * @return Self
     */
    public AbstractVisitor<T> withSemantics(Supplier<CodeSemantics> semanticsSupplier) {
        this.semanticsSupplier = ignore -> semanticsSupplier.get();
        return this;
    }

    /**
     * Tell the visitor that if it generates a token upon entering the entity, it should have semantics of type control. If
     * it doesn't generate a token, the semantics are discarded. This is not checked and does not lead to a warning.
     * @return Self
     */
    public AbstractVisitor<T> withControlSemantics() {
        this.semanticsSupplier = ignore -> CodeSemantics.createControl();
        return this;
    }

    /**
     * @param entity The entity to check.
     * @return Whether to visit the entity.
     */
    boolean matches(T entity) {
        return this.condition.test(entity);
    }

    /**
     * Enter a given entity, injecting the needed dependencies.
     */
    void enter(HandlerData<T> handlerData) {
        entryHandlers.forEach(handler -> handler.accept(handlerData));
    }

    void map(List<Consumer<HandlerData<T>>> handlers, TokenType tokenType, Function<T, Token> extractToken) {
        handlers.add(0, handlerData -> handlerData.collector().addToken(tokenType, semanticsSupplier, handlerData.entity(), extractToken,
                handlerData.variableRegistry()));
    }

    abstract Token extractEnterToken(T entity);
}