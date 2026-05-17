package ru.draen.stella.typecheck;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.*;

import java.util.*;
import java.util.stream.Collectors;

public class TypeCheckRegistry {
    private final LinkedList<Scope> scopeStack = new LinkedList<>();
    private Optional<StellaType> expectedType = Optional.empty();
    private boolean isDeclarationPass = false; //костыль для объявления функций (в стелле нету форвард-декларэйшина)
    private boolean typeReconstructionEnabled = false;
    private boolean universalTypesEnabled = false;

    private final ConstraintUnifier constraintUnifier = new ConstraintUnifier();

    public TypeCheckRegistry() {
        enterScope("GLOBAL");
    }


    public void enterScope(String marker) {
        scopeStack.push(new Scope(marker));
    }

    public void exitScope() {
        scopeStack.pop();
    }

    public void addExpectedType(StellaType expected) {
        expectedType = Optional.of(expected);
    }

    public Optional<StellaType> consumeExpectedType() {
        Optional<StellaType> res = expectedType;
        expectedType = Optional.empty();
        return res;
    }

    public boolean addVar(String name, StellaType type) {
        return Objects.requireNonNull(scopeStack.peek()).vars.put(name, type) != null;
    }
    public boolean addTypeVar(StellaType.NamedVar var) {
        return Objects.requireNonNull(scopeStack.peek()).typeVars.put(var.name(), var) != null;
    }

    public Optional<StellaType> getVar(String name) {
        for (Scope scope : scopeStack) {
            StellaType type = scope.vars.get(name);
            if (type != null) return Optional.of(type);
        }
        return Optional.empty();
    }
    public Optional<StellaType.NamedVar> getTypeVar(String name) {
        for (Scope scope : scopeStack) {
            StellaType.NamedVar var = scope.typeVars.get(name);
            if (var != null) return Optional.of(var);
        }
        return Optional.empty();
    }

    public void addConstraint(Constraint constraint) {
        constraintUnifier.add(constraint);
    }
    public void unifyConstraints() {
        var mappings = constraintUnifier.unify();
        List<StellaType> freshVars = StellaType.FreshVar.all();
        for (var mapping : mappings) {
            freshVars = freshVars.stream().map(var -> var.replace(mapping)).toList();
        }
        freshVars.forEach(StellaType::checkAmbiguity);
    }

    public boolean isDeclarationPass() {
        return isDeclarationPass;
    }

    public void setDeclarationPass(boolean declarationPass) {
        isDeclarationPass = declarationPass;
    }

    public boolean isTypeReconstructionEnabled() {
        return typeReconstructionEnabled;
    }

    public void setTypeReconstructionEnabled(boolean typeReconstructionEnabled) {
        this.typeReconstructionEnabled = typeReconstructionEnabled;
    }

    public boolean isUniversalTypesEnabled() {
        return universalTypesEnabled;
    }

    public void setUniversalTypesEnabled(boolean universalTypesEnabled) {
        this.universalTypesEnabled = universalTypesEnabled;
    }

    public StellaType fromAst(StellaParser.StellatypeContext ctx) {
        return switch (ctx) {
            case StellaParser.TypeParensContext ctx2 -> fromAst(ctx2.type_);
            case StellaParser.TypeBoolContext ignored -> new StellaType.Bool();
            case StellaParser.TypeNatContext ignored -> new StellaType.Nat();
            case StellaParser.TypeFunContext fun -> new StellaType.Func(fun.paramTypes.stream()
                    .map(this::fromAst)
                    .toList(),
                    fromAst(fun.returnType));
            case StellaParser.TypeUnitContext ignored -> new StellaType.Unit();
            case StellaParser.TypeTupleContext tuple -> new StellaType.Tuple(
                    tuple.types.stream().map(this::fromAst).toList()
            );
            case StellaParser.TypeRecordContext record -> new StellaType.Record(
                    record.fieldTypes.stream().collect(Collectors.toMap(
                            fieldType -> fieldType.label.getText(),
                            fieldType -> new StellaType.Record.Item(fieldType.label.getText(), fromAst(fieldType.type_)),
                            (fieldType1, fieldType2) -> {
                                throw new ErrorDuplicateRecordTypeFields(record, fieldType1.name());
                            }
                    )));
            case StellaParser.TypeSumContext sum -> new StellaType.Sum(
                    fromAst(sum.left),
                    fromAst(sum.right)
            );
            case StellaParser.TypeVariantContext variant -> new StellaType.Variant(
                    variant.fieldTypes.stream().collect(Collectors.toMap(
                            fieldType -> fieldType.label.getText(),
                            fieldType -> new StellaType.Variant.Item(fieldType.label.getText(),
                                    Optional.ofNullable(fieldType.type_).map(this::fromAst)),
                            (fieldType1, fieldType2) -> {
                                throw new ErrorDuplicateVariantTypeFields(variant, fieldType1.name());
                            }
                    ))
            );
            case StellaParser.TypeListContext list -> new StellaType.StellaList(fromAst(list.type_));
            case StellaParser.TypeAutoContext auto -> StellaType.FreshVar.create(auto);
            case StellaParser.TypeForAllContext forall -> {
                try {
                    this.enterScope(forall.getText());
                    yield new StellaType.Forall(
                            forall.types.stream().map(token -> {
                                StellaType.NamedVar var = new StellaType.NamedVar(token.getText());
                                if (this.addTypeVar(var))  {
                                    throw new ErrorDuplicateTypeParameter(var, forall);
                                }
                                return var;
                            }).toList(),
                            fromAst(forall.type_)
                    );
                } finally {
                    this.exitScope();
                }
            }
            case StellaParser.TypeVarContext var -> getTypeVar(var.name.getText()).orElseThrow(() -> new ErrorUndefinedTypeVariable(var));
            default -> throw new UnsupportedException();
        };
    }
    private record Scope(String marker, Map<String, StellaType> vars, Map<String, StellaType.NamedVar> typeVars) {
        private Scope(String marker) {
            this(marker, new HashMap<>(), new HashMap<>());
        }
    }
}
