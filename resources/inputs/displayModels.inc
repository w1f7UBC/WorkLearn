Define			DisplayModel			{ Pixels }
Pixels			Shape					{ PIXELS }
Define			DisplayModel			{ Rectangle }
Rectangle		Shape					{ RECTANGLE }
Define				DisplayModel		{ Circle }
Circle				Shape				{ CIRCLE }
Define				DisplayModel		{ Arrow2D }
Arrow2D				Shape				{ ARROW2D }
Define				DisplayModel		{ Triangle }
Triangle			Shape				{ TRIANGLE }

Define				DisplayModel		{ BarGauge2D }
BarGauge2D			Shape				{ BARGAUGE2D }

Define				ColladaModel		{ CrossHairs }
CrossHairs			ColladaFile			{ '<res>/shapes/CrossHairs.dae' }

Define		ColladaModel	{ Cube }
Cube		ColladaFile		{ '<res>/shapes/cube.dae' }
DisplayEntity	DefaultDisplayModel { Cube }

Define		ColladaModel	{ Sphere }
Sphere		ColladaFile		{ '<res>/shapes/Sphere.dae' }


Define ColladaModel { Truck3D }
Truck3D ColladaFile { '<res>/ROLOS_resources/Shapes/Sin t_tulo.dae' }

Define ColladaModel { Cargo3D }
Cargo3D ColladaFile { '<res>/ROLOS_resources/Shapes/WoodchipTrailer.dae' }

Define ColladaModel { CargoContent3D }
CargoContent3D ColladaFile { '<res>/ROLOS_resources/Shapes/CargoContent.dae' }

Define ColladaModel { StockpileContent1 }
StockpileContent1 ColladaFile { '<res>/ROLOS_resources/Shapes/Woodpile.dae' }

Define ColladaModel { StockpileContent2 }
StockpileContent2 ColladaFile { '<res>/ROLOS_resources/Shapes/WoodChipPile2.dae' }

Define ColladaModel { LoaderBodyModel }
LoaderBodyModel ColladaFile { '<res>/ROLOS_resources/Shapes/LoaderBody.dae' }

Define ColladaModel { LoaderBaseModel }
LoaderBaseModel ColladaFile { '<res>/ROLOS_resources/Shapes/LoaderBase.dae' }

Define ColladaModel { WoodChipperModel }
WoodChipperModel ColladaFile { '<res>/ROLOS_resources/Shapes/AutoSave_cone.dae' }
WoodChipperModel ModelScale { 5 5 5 }

