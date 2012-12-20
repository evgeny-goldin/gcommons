
ruleset {

    description 'GCommons CodeNarc RuleSet'

    ruleset( 'http://codenarc.sourceforge.net/StarterRuleSet-AllRulesByCategory.groovy.txt' ) {

        AbcMetric                ( enabled : false )
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
        SpaceBeforeOpeningBrace  ( enabled : false )
        SpaceBeforeClosingBrace  ( enabled : false )
        SpaceAfterClosingBrace   ( enabled : false )
        SpaceAfterOpeningBrace   ( enabled : false )
        SpaceAfterComma          ( enabled : false )

        VariableName             ( finalRegex : /\w+/ )
        LineLength               ( length     : 190   )
    }
}
