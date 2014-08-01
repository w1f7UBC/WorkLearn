"Define Palette { 'ROLOS' }

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
                  }
Group                       JavaClass { com.sandwell.JavaSimulation.Group               } Palette { 'ROLOS' } DragAndDrop 			{ FALSE 	}
TimeSeries                  JavaClass { com.sandwell.JavaSimulation.TimeSeries          } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
FacilityGeneralManager      JavaClass { com.ROLOS.DMAgents.FacilityGeneralManager       } Palette { 'ROLOS' } DragAndDrop 			{ FALSE 	}
FacilityOperationsManager   JavaClass { com.ROLOS.DMAgents.FacilityOperationsManager    } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
FacilityFinancialManager    JavaClass { com.ROLOS.DMAgents.FacilityFinancialManager     } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
FacilityScheduleManager     JavaClass { com.ROLOS.DMAgents.FacilityScheduleManager      } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
FacilityTransportationManager     JavaClass { com.ROLOS.DMAgents.FacilityTransportationManager      } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
SimulationManager           JavaClass { com.ROLOS.DMAgents.SimulationManager            } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
RouteManager                JavaClass { com.ROLOS.DMAgents.RouteManager                 } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
TrafficController           JavaClass { com.ROLOS.DMAgents.TrafficController                     } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
Contract                    JavaClass { com.ROLOS.Economic.Contract                     } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
Market                      JavaClass { com.ROLOS.Economic.Market                         } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
ReportAgent                 JavaClass { com.ROLOS.Logistics.ReportAgent                 } Palette { 'ROLOS' } DragAndDrop 			{ FALSE 	}
Facility                    JavaClass { com.ROLOS.Logistics.Facility                    } Palette { 'ROLOS' } DefaultDisplayModel 	{ Circle 				}
Transshipment               JavaClass { com.ROLOS.Logistics.Transshipment            } Palette { 'ROLOS' } DefaultDisplayModel 	{ Circle 				}
Fleet                       JavaClass { com.ROLOS.Logistics.Fleet                       } Palette { 'ROLOS' } DefaultDisplayModel 	{ Circle 				}
EntityGenerator             JavaClass { com.ROLOS.Logistics.EntityGenerator             } Palette { 'ROLOS' } DefaultDisplayModel 	{ Circle 				}
MovingEntity                JavaClass { com.ROLOS.Logistics.MovingEntity                } Palette { 'ROLOS' } DefaultDisplayModel 	{ Truck3D 				}
BulkCargo                   JavaClass { com.ROLOS.Logistics.BulkCargo                   } Palette { 'ROLOS' } DefaultDisplayModel 	{ Cargo3D 				}
Loader                      JavaClass { com.ROLOS.Logistics.Loader                      } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
BulkMaterial                JavaClass { com.ROLOS.Logistics.BulkMaterial                } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
BulkMaterialProcessor       JavaClass { com.ROLOS.Logistics.BulkMaterialProcessor       } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
Stockpile                   JavaClass { com.ROLOS.Logistics.Stockpile                   } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
RouteSegment                JavaClass { com.ROLOS.Logistics.RouteSegment                 } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
RouteEntity                 JavaClass { com.ROLOS.Logistics.RouteEntity                 } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
ParkBlock                   JavaClass { com.ROLOS.Logistics.ParkBlock                   } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
LoadingBay                  JavaClass { com.ROLOS.Logistics.LoadingBay                  } Palette { 'ROLOS' } DefaultDisplayModel 	{ ScreenPointsModelDefault 				}
Delay                       JavaClass { com.ROLOS.Logistics.Delay                       } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
EntitySource                JavaClass { com.ROLOS.Logistics.EntitySource                       } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}
EntitySink                  JavaClass { com.ROLOS.Logistics.EntitySink                       } Palette { 'ROLOS' } DragAndDrop 	{ FALSE 	}

Define ReportAgent { Report }
Define SimulationManager { PlanningManager }
Define RouteManager { TransportationManager }