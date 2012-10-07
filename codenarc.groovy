
ruleset {

    description 'GCommons CodeNarc RuleSet'

    ruleset( 'http://codenarc.sourceforge.net/StarterRuleSet-AllRulesByCategory.groovy.txt' ) {

        DuplicateNumberLiteral   ( enabled : false )
        DuplicateStringLiteral   ( enabled : false )
        DuplicateListLiteral     ( enabled : false )
        BracesForClass           ( enabled : false )
        BracesForMethod          ( enabled : false )
        BracesForIfElse          ( enabled : false )
        BracesForForLoop         ( enabled : false )
        BracesForTryCatchFinally ( enabled : false )
        JavaIoPackageAccess      ( enabled : false )
        MethodName               ( enabled : false )
        UnnecessarySubstring     ( enabled : false )
        ConfusingMethodName      ( enabled : false )
        ThrowRuntimeException    ( enabled : false )
        FactoryMethodName        ( enabled : false )
        CatchThrowable           ( enabled : false )

        VariableName             ( finalRegex : /\w+/ )
        LineLength               ( length     : 190   )
    }
}
