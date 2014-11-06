Define ObjectType	{
                    BarGraphModel
				    ScatterGraphTester
                    ScatterGraphModel          
                  	}
                  
ScatterGraphTester	   		JavaClass { Graphs.ScatterGraphTester	} Palette { 'Graph' } DragAndDrop 	{ TRUE  }
ScatterGraphModel		   	JavaClass { Graphs.ScatterGraphModel	} Palette { 'Graph' } DragAndDrop 	{ FALSE }
BarGraphModel          		JavaClass { Graphs.BarGraphModel   		} Palette { 'Graph' } DragAndDrop 	{ FALSE }  

Define ScatterGraphModel	{ ScatterGraphModelDefault	}
Define BarGraphModel       	{ BarGraphModelDefault     	}

Define ObjectType	{
					ScatterGraph
					BarGraph
		  			}
				
ScatterGraph				JavaClass { Graphs.ScatterGraph			} Palette { 'Graph' } DefaultDisplayModel 	{ ScatterGraphModelDefault 	}
BarGraph					JavaClass { Graphs.BarGraph				} Palette { 'Graph' } DefaultDisplayModel 	{ BarGraphModelDefault    	}