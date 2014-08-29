
Define ObjectType {
                    DataBaseObject
                    InventoryQuery 
					ExcelInput
					ExcelTest
					}
DataBaseObject	JavaClass { DataBase.DataBaseObject               } Palette { 'database' } DefaultDisplayModel 	{ Circle 				}

InventoryQuery JavaClass { DataBase.InventoryQuery              } Palette { 'database' } DefaultDisplayModel 	{ Circle 				}
ExcelInput JavaClass{ DataBase.ExcelInput              } Palette { 'database' } DefaultDisplayModel 	{ Circle 				}
ExcelTest JavaClass{ DataBase.ExcelTest 		} Palette { 'database' } DefaultDisplayModel 	{ Circle 				}
Define DataBaseObject { InventoryDataBase }
Define InventoryQuery { InventoryQueryObject }
Define ExcelInput{ ExcelInput1 }
Define ExcelTest{ ExcelTest1 }
InventoryQueryObject TargetDatabase { InventoryDataBase }
ExcelTest1 targetexcel{ExcelInput1}

