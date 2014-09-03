
Define ObjectType {
					DatabaseObject
			    	Query
                    InventoryQuery 
					ExcelInput
					ExcelTest
					}
					
DatabaseObject	JavaClass { DataBase.Database }		 	Palette { 'database' } DefaultDisplayModel { Circle }
Query 			JavaClass { DataBase.Query } 			Palette { 'database' } DefaultDisplayModel { Circle }
InventoryQuery 	JavaClass { DataBase.InventoryQuery } 	Palette { 'database' } DefaultDisplayModel { Circle }
ExcelInput 		JavaClass { DataBase.ExcelInput } 		Palette { 'database' } DefaultDisplayModel { Circle }
ExcelTest 		JavaClass { DataBase.ExcelTest } 		Palette { 'database' } DefaultDisplayModel { Circle }

Define DatabaseObject { InventoryDatabase }
Define InventoryQuery { InventoryQueryObject }
Define ExcelInput { ExcelInput1 }
Define ExcelTest { ExcelTest1 }

ExcelTest1 targetexcel { ExcelInput1 }

