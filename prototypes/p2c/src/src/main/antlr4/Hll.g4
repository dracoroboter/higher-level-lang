grammar Hll;

// ===== Parser Rules =====

program
    : declaration* EOF
    ;

declaration
    : typeDecl
    | unionTypeDecl
    | structDecl
    | fnDecl
    | importDecl
    | testDecl
    ;

// --- Type declarations ---
typeDecl
    : 'type' IDENT '=' typeExpr ('where' whereConstraint)?
    ;

whereConstraint
    : qualifiedCall
    ;

qualifiedCall
    : IDENT ('.' IDENT)* '(' args? ')'
    ;

// --- Union type (for errors) ---
unionTypeDecl
    : 'type' IDENT '=' IDENT ('|' IDENT)+
    ;

// --- Struct declarations ---
structDecl
    : 'struct' IDENT '{' fieldDecl (',' fieldDecl)* '}'
    ;

fieldDecl
    : typeExpr IDENT
    ;

// --- Function declarations ---
fnDecl
    : 'function' IDENT '(' params? ')' ('->' typeExpr)? failsClause? block
    ;

failsClause
    : 'fails' IDENT (',' IDENT)*
    ;

params
    : param (',' param)*
    ;

param
    : typeExpr IDENT
    ;

// --- Import ---
importDecl
    : 'import' 'java' STRING 'as' IDENT ('{' javaMapping* '}')?
    | 'import' qualifiedName 'as' IDENT
    ;

testDecl
    : 'test' STRING block
    ;
qualifiedName
    : IDENT ('.' IDENT)*
    ;

javaMapping
    : 'function' IDENT '(' params? ')' '->' typeExpr
    ;

// --- Types ---
typeExpr
    : IDENT ('<' typeExpr (',' typeExpr)? '>')?
    ;

// --- Statements ---
block
    : '{' statement* '}'
    ;

statement
    : letStmt
    | returnStmt
    | whileStmt
    | assignStmt
    | ifStmt
    | assertStmt
    | expectErrorStmt
    | exprStmt
    ;

letStmt
    : 'let' 'mut'? typeExpr? IDENT '=' expr errorHandler*
    ;

errorHandler
    : '|' pattern '=>' (expr | block)
    ;

returnStmt
    : 'return' expr?
    ;

matchArm
    : pattern '=>' (expr | block)
    ;

pattern
    : patternName '(' IDENT ')'     // Some(x), Ok(x), Err(e), InvalidEmail(e)
    | patternName '(' pattern ')'   // Err(InvalidEmail(e)) — nested
    | 'None'
    | 'true'
    | 'false'
    | IDENT
    | '_'
    ;

patternName
    : IDENT | 'Some' | 'Ok' | 'Err'
    ;

whileStmt
    : 'while' expr block
    ;

assertStmt
    : 'assert' expr
    ;

expectErrorStmt
    : 'expect_error' block
    ;
assignStmt
    : IDENT '=' expr
    ;
ifStmt
    : 'if' expr block ('else' block)?
    ;

exprStmt
    : expr
    ;

// --- Expressions ---
expr
    : primary                                   # primaryExpr
    | expr '.' IDENT                            # fieldAccess
    | expr '.' IDENT '(' args? ')'             # methodCall
    | expr '?'                                  # optionPropagate
    | expr op=('*' | '/' | '%') expr            # mulDiv
    | expr op=('+' | '-') expr                  # addSub
    | expr op=('==' | '!=' | '<' | '>' | '<=' | '>=') expr  # comparison
    | expr op='&&' expr                         # logicAnd
    | expr op='||' expr                         # logicOr
    | '!' expr                                  # logicNot
    | 'match' expr '{' matchArm+ '}'           # matchExpr
    ;

primary
    : IDENT '(' args? ')'                       # fnCall
    | 'Ok' '(' expr ')'                         # okExpr
    | 'Err' '(' expr ')'                        # errExpr
    | 'Some' '(' expr ')'                       # someExpr
    | 'None'                                    # noneLit
    | 'fail' IDENT '(' args? ')'               # failExpr
    | IDENT                                     # identifier
    | STRING                                    # stringLit
    | NUMBER                                    # numberLit
    | FLOAT                                     # floatLit
    | ('true' | 'false')                        # boolLit
    | '(' expr ')'                              # parenExpr
    ;

args
    : expr (',' expr)*
    ;

// ===== Lexer Rules =====

// Keywords (order matters — longer first)
TYPE    : 'type' ;
STRUCT  : 'struct' ;
FAILS   : 'fails' ;
FAIL    : 'fail' ;
FN      : 'function' ;
LET     : 'let' ;
RETURN  : 'return' ;
MATCH   : 'match' ;
IF      : 'if' ;
ELSE    : 'else' ;
WHILE   : 'while' ;
MUT     : 'mut' ;
IMPORT  : 'import' ;
JAVA    : 'java' ;
AS      : 'as' ;
WHERE   : 'where' ;
NONE    : 'None' ;
SOME    : 'Some' ;
OK      : 'Ok' ;
ERR     : 'Err' ;
TRUE    : 'true' ;
FALSE   : 'false' ;

// Identifiers and literals
IDENT   : [a-zA-Z_][a-zA-Z0-9_]* ;
STRING  : '"' (~["\\\r\n] | '\\' .)* '"' ;
FLOAT   : [0-9]+ '.' [0-9]+ ;
NUMBER  : [0-9]+ ;

// Symbols
ARROW   : '->' ;
FATARROW: '=>' ;
QMARK   : '?' ;
DOT     : '.' ;
COMMA   : ',' ;
COLON   : ':' ;
LPAREN  : '(' ;
RPAREN  : ')' ;
LBRACE  : '{' ;
RBRACE  : '}' ;
LT      : '<' ;
GT      : '>' ;
EQ      : '==' ;
NEQ     : '!=' ;
ASSIGN  : '=' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
PERCENT : '%' ;
AND     : '&&' ;
OR      : '||' ;
NOT     : '!' ;
LE      : '<=' ;
GE      : '>=' ;
PIPE    : '|' ;

// Whitespace and comments
WS      : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
