
Define ObjectType {
					Database
			    	Query
                    InventoryQuery 
					ExcelInput
					ExcelTest
					TestEntity
					WorldView
					}
					
Database		JavaClass { DataBase.Database }			Palette { 'database' } 
Query 			JavaClass { DataBase.Query } 			Palette { 'database' } 
InventoryQuery 	JavaClass { DataBase.InventoryQuery } 	Palette { 'database' } 
ExcelInput 		JavaClass { DataBase.ExcelInput } 		Palette { 'database' } 			DefaultDisplayModel { Circle }
ExcelTest 		JavaClass { DataBase.ExcelTest } 		Palette { 'database' } 			DefaultDisplayModel { Circle }
TestEntity		JavaClass { DataBase.TestEntity }			
WorldView	 	JavaClass { worldwind.WorldView }		Palette { 'Graphics Objects' } 	DragAndDrop { FALSE }

Define Database { InventoryDatabase }
Define InventoryQuery { InventoryQueryObject }
InventoryQueryObject TargetDatabase { InventoryDatabase }
Define ExcelInput{ ExcelInput1 }
Define ExcelTest{ ExcelTest1 }
"ExcelTest1 targetexcel{ExcelInput1}

