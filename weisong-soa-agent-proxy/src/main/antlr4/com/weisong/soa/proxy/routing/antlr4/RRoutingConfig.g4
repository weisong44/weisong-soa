grammar RRoutingConfig;

routing:
	(target_group | route | NL)* route_otherwise (NL)* EOF;
	
/****************************
  Target group
****************************/
target_group:
	target_group_def (load_balancing)* (target)+
  ;
target_group_def:
	'target-group' target_group_name NL
  ;
target_group_name: WORD
  ;
load_balancing:
	'load-balancing' load_balancing_type NL
  ;
load_balancing_type: WORD
  ;
target:
	'target' target_value ('weight' target_weight_value)? NL
  ;
target_value: TARGET
  ;
target_weight_value: NUM
  ;

/****************************
  Route
****************************/
route:
	route_def match (forward_to)+
  ;
route_def:
	'route' route_name NL
  ;
route_name: WORD
  ;
forward_to:
	(drop | ('forward-to' forward_to_dest ('weight' forward_weight_value)?)) NL
  ;
match:
	'match' match_value NL
  ;
match_value: ('any' | 'none' | PATTERN)
  ;
drop: 'drop'
  ;
forward_to_dest: WORD
  ;
forward_weight_value: NUM
  ;
route_otherwise:
	route_otherwise_def match (forward_to)+
  ;
route_otherwise_def:
	'route otherwise' NL
  ;

/****************************
  Tokens
****************************/
DIGIT: [0-9]
  ;
NUM:
	[+-]? (DIGIT)* '.' DIGIT+
  |	[+-]? DIGIT+
  ;
OCTET: DIGIT? DIGIT? DIGIT
  ;
IP_ADDR: OCTET '.' OCTET '.' OCTET '.' OCTET
  ;
PORT: DIGIT? DIGIT? DIGIT? DIGIT? DIGIT
  ;
TARGET: IP_ADDR ':' PORT
  ;
WORD: [a-zA-Z][a-zA-Z0-9-_]*
  ;
PATTERN: '"' [a-zA-Z0-9-=._~ ]+ '"'
  ;
WS: (' ' | '\t')+ -> skip
  ;
NL: ('\r' | '\n')+ 
  ;
COMMENT: '#' .*? NL -> skip 
  ;