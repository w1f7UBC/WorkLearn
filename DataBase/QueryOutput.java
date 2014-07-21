package DataBase;



public @interface QueryOutput {
	public String name();
	public String description() default "";
	public Class<? extends Query> unitType() ;
}
