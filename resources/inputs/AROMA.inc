"Define Palette { 'AROMA' }

Define ObjectType {
                    ReportAgent
                    Group
                    FacilityGeneralManager
                    FacilityFinancialManager
                    FacilityOperationsManager
                    FacilityScheduleManager
                    FacilityTransportationManager
                    SimulationManager
                    Contract
                    Market
                    Facility
                    Transshipment
                    Fleet
                    EntityGenerator
                    MovingEntity
                    BulkCargo
                    Loader
                    BulkMaterial
                    BulkMaterialProcessor
                    Stockpile
                    RouteSegment
                    RouteEntity
                    EntitySource
                    EntitySink
                    LoadingBay
                    RouteManager
                    TrafficController
                    ParkBlock
                    TimeSeries
                    Delay
                    SankyInfo
                    XYGraph
                    BarGraphModel
		    AROMAGraphTester
                    AROMAGraphModel          
                  }
                  
Group                       	JavaClass { com.sandwell.JavaSimulation.Group               } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
XYGraph                     	JavaClass { com.sandwell.JavaSimulation3D.XYGraph           } Palette { 'AROMA' } DragAndDrop 	{ TRUE 	}
TimeSeries                  	JavaClass { com.sandwell.JavaSimulation.TimeSeries          } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
FacilityGeneralManager      	JavaClass { com.AROMA.DMAgents.FacilityGeneralManager       } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
FacilityOperationsManager   	JavaClass { com.AROMA.DMAgents.FacilityOperationsManager    } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
FacilityFinancialManager    	JavaClass { com.AROMA.DMAgents.FacilityFinancialManager     } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
FacilityScheduleManager     	JavaClass { com.AROMA.DMAgents.FacilityScheduleManager      } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
FacilityTransportationManager   JavaClass { com.AROMA.DMAgents.FacilityTransportationManager} Palette { 'AROMA' } DragAndDrop 	{ FALSE }
SimulationManager           	JavaClass { com.AROMA.DMAgents.SimulationManager            } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
RouteManager                	JavaClass { com.AROMA.DMAgents.RouteManager                 } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
TrafficController           	JavaClass { com.AROMA.DMAgents.TrafficController            } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
Contract                    	JavaClass { com.AROMA.Economic.Contract                     } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
Market                      	JavaClass { com.AROMA.Economic.Market                       } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
ReportAgent                 	JavaClass { com.AROMA.Logistics.ReportAgent                 } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
Facility                    	JavaClass { com.AROMA.Logistics.Facility                    } Palette { 'AROMA' } DefaultDisplayModel 	{ Circle 								}
Transshipment               	JavaClass { com.AROMA.Logistics.Transshipment            	} Palette { 'AROMA' } DefaultDisplayModel 	{ Circle 								}
Fleet                       	JavaClass { com.AROMA.Logistics.Fleet                       } Palette { 'AROMA' } DefaultDisplayModel 	{ Circle 								}
EntityGenerator             	JavaClass { com.AROMA.Logistics.EntityGenerator             } Palette { 'AROMA' } DefaultDisplayModel 	{ Circle 								}
"MovingEntity               	JavaClass { com.AROMA.Logistics.MovingEntity                } Palette { 'AROMA' } DefaultDisplayModel 	{ Truck3D 								}
MovingEntity                	JavaClass { com.AROMA.Logistics.MovingEntity                } Palette { 'AROMA' } DefaultDisplayModel 	{ BioenergyIcon							}
BulkCargo                   	JavaClass { com.AROMA.Logistics.BulkCargo                   } Palette { 'AROMA' } DefaultDisplayModel 	{ Cargo3D 								}
Loader                      	JavaClass { com.AROMA.Logistics.Loader                      } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
BulkMaterial                	JavaClass { com.AROMA.Logistics.BulkMaterial                } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
BulkMaterialProcessor       	JavaClass { com.AROMA.Logistics.BulkMaterialProcessor       } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
Stockpile                   	JavaClass { com.AROMA.Logistics.Stockpile                   } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
RouteSegment                	JavaClass { com.AROMA.Logistics.RouteSegment                } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
RouteEntity                 	JavaClass { com.AROMA.Logistics.RouteEntity                 } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
ParkBlock                   	JavaClass { com.AROMA.Logistics.ParkBlock                   } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
LoadingBay                  	JavaClass { com.AROMA.Logistics.LoadingBay                  } Palette { 'AROMA' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
Delay                       	JavaClass { com.AROMA.Logistics.Delay                       } Palette { 'AROMA' } DragAndDrop	{ FALSE }
EntitySource                	JavaClass { com.AROMA.Logistics.EntitySource                } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
EntitySink                  	JavaClass { com.AROMA.Logistics.EntitySink                  } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
SankyInfo                  	JavaClass { com.AROMA.Logistics.SankeyInfo                  } Palette { 'AROMA' } DragAndDrop 	{ FALSE }
AROMAGraphTester	   	JavaClass { com.AROMA.AROMAGraphTester						} Palette { 'AROMA' } DragAndDrop 	{ TRUE  }
AROMAGraphModel		   	JavaClass { com.AROMA.AROMAGraphModel						} Palette { 'AROMA' } DragAndDrop 	{ FALSE }
BarGraphModel          		JavaClass { com.AROMA.DisplayModel.BarGraphModel            } Palette { 'AROMA' } DragAndDrop 	{ FALSE }  

Define AROMAGraphModel	   	{ AROMAGraphModelDefault	}
Define BarGraphModel       	{ BarGraphModelDefault     	}
Define ReportAgent 	 	{ Report 			}
Define SimulationManager	{ PlanningManager 		}
Define RouteManager 		{ TransportationManager 	}

Define ObjectType {
			AROMAGraph
			AROMABarGraph
		   }
				
AROMAGraph			  			JavaClass { com.AROMA.AROMAGraph							} Palette { 'AROMA' } DefaultDisplayModel 	{ AROMAGraphModelDefault 				}
AROMABarGraph						JavaClass { com.AROMA.AROMABarGraph							} Palette { 'AROMA' } DefaultDisplayModel 	{ BarGraphModelDefault     				}