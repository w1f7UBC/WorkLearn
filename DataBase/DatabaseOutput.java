package DataBase;



public @interface DatabaseOutput {
	public String name();
	public String description() default "";
	public Class<? extends AbstractQuery> unitType() ;
}
