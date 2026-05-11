grammar Hll;

// ===== Parser Rules =====

program
    : moduleDecl? declaration* EOF
    ;

moduleDecl
    : 'module' qualifiedName
    ;

declaration
    : typeDecl
    | unionTypeDecl
    | structDecl
    | stateDecl
    | effectDecl
    | serviceDecl
    | provideDecl
    | fnDecl
    | exportDecl
    | importDecl
    | testDecl
    ;

// --- Export wrapper ---
exportDecl
    : 'export' (fnDecl | structDecl | typeDecl | serviceDecl | provideDecl | stateDecl)
    ;

// --- Service (interface) ---
serviceDecl
    : 'service' IDENT '{' serviceFn+ '}'
    ;

serviceFn
    : 'function' IDENT '(' params? ')' '->' typeExpr failsClause?
    ;

// --- Provide (implementation of a service) ---
provideDecl
    : 'provide' IDENT '{' provideBody+ '}'
    ;

provideBody
    : needsDecl
    | fnDecl
    ;

needsDecl
    : 'needs' IDENT IDENT
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

// --- Effect declarations ---
effectDecl
    : 'effect' IDENT '{' fieldDecl (',' fieldDecl)* '}'
    ;

// --- State declarations ---
stateDecl
    : 'state' IDENT '{' stateVariant+ '}'
    ;

stateVariant
    : IDENT '{' stateFn* '}'
    ;

stateFn
    : 'function' IDENT '(' params? ')' '->' IDENT
    ;

fieldDecl
    : typeExpr IDENT
    ;

// --- Function declarations ---
fnDecl
    : 'function' IDENT '(' params? ')' ('->' typeExpr)? failsClause? effectsClause? block
    ;

failsClause
    : 'fails' IDENT (',' IDENT)*
    ;

effectsClause
    : 'effects' '{' IDENT (',' IDENT)* '}'
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
    | 'import' qualifiedName ('.' '{' importList '}')?
    | 'import' qualifiedName 'as' IDENT
    ;

importList
    : IDENT (',' IDENT)*
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
    | ifStmt
    | whileStmt
    | forInStmt
    | assignStmt
    | assertStmt
    | expectErrorStmt
    | expectFailStmt
    | mockStmt
    | exprStmt
    ;

forInStmt
    : 'for' IDENT 'in' expr block
    ;

mockStmt
    : 'mock' IDENT '{' fnDecl+ '}'
    ;

letStmt
    : 'let' 'mut'? typeExpr? IDENT '=' expr errorHandler?
    ;

errorHandler
    : ('|' IDENT '(' IDENT ')' '=>' expr)+
    ;

returnStmt
    : 'return' expr?
    ;

matchArm
    : pattern '=>' (expr | block)
    ;

pattern
    : patternName '(' IDENT ')'
    | patternName '(' pattern ')'
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

expectFailStmt
    : 'expectFail' IDENT block
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
    | expr '.' IDENT '(' args? ')'             # methodCall
    | expr '.' IDENT                            # fieldAccess
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
    | 'raise' IDENT '(' args? ')'              # raiseExpr
    | 'handle' expr '{' matchArm+ '}'          # handleExpr
    | 'spawn' IDENT                             # spawnExpr
    | 'await' expr                              # awaitExpr
    | '|' IDENT '|' expr                        # lambdaExpr
    | '|' IDENT ',' IDENT '|' expr              # lambda2Expr
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

// Keywords
MODULE  : 'module' ;
EXPORT  : 'export' ;
SERVICE : 'service' ;
PROVIDE : 'provide' ;
NEEDS   : 'needs' ;
MOCK    : 'mock' ;
SPAWN   : 'spawn' ;
AWAIT   : 'await' ;
FAILS   : 'fails' ;
FAIL    : 'fail' ;
TYPE    : 'type' ;
STRUCT  : 'struct' ;
STATE   : 'state' ;
EFFECT  : 'effect' ;
EFFECTS : 'effects' ;
HANDLE  : 'handle' ;
RAISE   : 'raise' ;
FN      : 'function' ;
LET     : 'let' ;
RETURN  : 'return' ;
MATCH   : 'match' ;
IF      : 'if' ;
ELSE    : 'else' ;
WHILE   : 'while' ;
FOR     : 'for' ;
IN      : 'in' ;
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
TEST    : 'test' ;
ASSERT  : 'assert' ;
EXPECT_ERROR : 'expect_error' ;
EXPECT_FAIL  : 'expectFail' ;

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
