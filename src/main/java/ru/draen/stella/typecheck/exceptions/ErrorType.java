package ru.draen.stella.typecheck.exceptions;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ErrorType {
    ERROR_MISSING_MAIN(1, ErrorMissingMain.class),
    ERROR_UNDEFINED_VARIABLE(2, ErrorUndefinedVariable.class),
    ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION(3, ErrorUnexpectedTypeForExpression.class),
    ERROR_NOT_A_FUNCTION(4, ErrorNotAFunction.class),
    ERROR_NOT_A_TUPLE(5, ErrorNotATuple.class),
    ERROR_NOT_A_RECORD(6, ErrorNotARecord.class),
    ERROR_NOT_A_LIST(7, ErrorNotAList.class),
    ERROR_UNEXPECTED_LAMBDA(8, ErrorUnexpectedLambda.class),
    ERROR_UNEXPECTED_TYPE_FOR_PARAMETER(9, ErrorUnexpectedTypeForParameter.class),
    ERROR_UNEXPECTED_TUPLE(10, ErrorUnexpectedTuple.class),
    ERROR_UNEXPECTED_RECORD(11, ErrorUnexpectedRecord.class),
    ERROR_UNEXPECTED_VARIANT(12, ErrorUnexpectedVariant.class),
    ERROR_UNEXPECTED_LIST(13, ErrorUnexpectedList.class),
    ERROR_UNEXPECTED_INJECTION(14, ErrorUnexpectedInjection.class),
    ERROR_MISSING_RECORD_FIELDS(15, ErrorMissingRecordFields.class),
    ERROR_UNEXPECTED_RECORD_FIELDS(16, ErrorUnexpectedRecordFields.class),
    ERROR_UNEXPECTED_FIELD_ACCESS(17, ErrorUnexpectedFieldAccess.class),
    ERROR_UNEXPECTED_VARIANT_LABEL(18, ErrorUnexpectedVariantLabel.class),
    ERROR_TUPLE_INDEX_OUT_OF_BOUNDS(19, ErrorTupleIndexOutOfBounds.class),
    ERROR_UNEXPECTED_TUPLE_LENGTH(20, ErrorUnexpectedTupleLength.class),
    ERROR_AMBIGUOUS_SUM_TYPE(21, ErrorAmbiguousSumType.class),
    ERROR_AMBIGUOUS_VARIANT_TYPE(22, ErrorAmbiguousVariantType.class),
    ERROR_AMBIGUOUS_LIST(23, ErrorAmbiguousList.class),
    ERROR_ILLEGAL_EMPTY_MATCHING(24, ErrorIllegalEmptyMatching.class),
    ERROR_NONEXHAUSTIVE_MATCH_PATTERNS(25, ErrorNonexhaustiveMatchPatterns.class),
    ERROR_UNEXPECTED_PATTERN_FOR_TYPE(26, ErrorUnexpectedPatternForType.class),
    ERROR_DUPLICATE_RECORD_FIELDS(27, ErrorDuplicateRecordFields.class),
    ERROR_DUPLICATE_RECORD_TYPE_FIELDS(28, ErrorDuplicateRecordTypeFields.class),
    ERROR_DUPLICATE_VARIANT_TYPE_FIELDS(29, ErrorDuplicateVariantTypeFields.class),
    ERROR_DUPLICATE_FUNCTION_DECLARATION(30, ErrorDuplicateFunctionDeclaration.class),
    //extra

    ERROR_INCORRECT_NUMBER_OF_ARGUMENTS(-1, ErrorIncorrectNumberOfArguments.class, true),
    ERROR_INCORRECT_ARITY_OF_MAIN(-1, ErrorIncorrectArityOfMain.class, true),
    ERROR_UNEXPECTED_NUMBER_OF_PARAMETERS_IN_LAMBDA(-1, ErrorUnexpectedNumberOfParametersInLambda.class, true),
    ERROR_DUPLICATE_RECORD_PATTERN_FIELDS(-1, ErrorDuplicateRecordPatternFields.class, true),
    ERROR_NONEXHAUSTIVE_LET_PATTERNS(-1, ErrorNonexhaustiveLetPatterns.class, true),
    ERROR_NONEXHAUSTIVE_LET_REC_PATTERNS(-1, ErrorNonexhaustiveLetRecPatterns.class, true),
    ERROR_AMBIGUOUS_PATTERN_TYPE(-1, ErrorAmbiguousPatternType.class, true),
    ERROR_MISSING_DATA_FOR_LABEL(-1, ErrorMissingDataForLabel.class, true),
    ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL(-1, ErrorUnexpectedDataForNullaryLabel.class, true),
    ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN(-1, ErrorUnexpectedNonNullaryVariantPattern.class, true),
    ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN(-1, ErrorUnexpectedNullaryVariantPattern.class, true),
    ERROR_DUPLICATE_LET_BINDING(-1, ErrorDuplicateLetBinding.class, true),
    ERROR_DUPLICATE_FUNCTION_PARAMETER(-1, ErrorDuplicateFunctionParameter.class, true),
    ERROR_DUPLICATE_PATTERN_VARIABLE(-1, ErrorDuplicatePatternVariable.class, true)
    ;

    private final int number;
    private final boolean isExtra;
    private final Class<? extends TypeCheckException> errorClass;

    private final static Map<Class<? extends TypeCheckException>, ErrorType> byExceptionClass = Arrays.stream(ErrorType.values())
            .collect(Collectors.toMap(
                    errorType -> errorType.errorClass,
                    Function.identity()
    ));

    ErrorType(int number, Class<? extends TypeCheckException> errorClass) {
        this.number = number;
        this.errorClass = errorClass;
        this.isExtra = false;
    }

    ErrorType(int number, Class<? extends TypeCheckException> errorClass, boolean isExtra) {
        this.number = number;
        this.errorClass = errorClass;
        this.isExtra = isExtra;
    }

    public int getNumber() {
        return number;
    }

    public Class<? extends TypeCheckException> getErrorClass() {
        return errorClass;
    }

    public boolean isExtra() {
        return isExtra;
    }

    public static ErrorType getByException(Class<? extends TypeCheckException> e) {
        return byExceptionClass.get(e);
    }
}
