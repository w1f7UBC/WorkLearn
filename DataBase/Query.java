package DataBase;



public @interface Query {
	public String name();
	public String description() default "";
	public Class<? extends AbstractQuery> unitType() ;
}
