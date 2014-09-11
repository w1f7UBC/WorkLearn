package worldwind;

import java.io.File;
import java.io.IOException;

public class LayerManager {
	//using a 32bit (64bit backwards compatible) pgsql2shp.exe to do the conversion
	private final String exe=System.getProperty("user.dir")+"\\resources\\exe\\pgsql2shp.exe";
	//stores the generated shp files inside this directory, be aware this can get really big as many shape files are generated
	//ideally i want it to directly generate and load the shape file onto the canvas without creating a physical shp file, however i never figured out how to do that
	private final String destination=System.getProperty("user.dir")+"\\resources\\temp";
	private String ip=null;
	private String port=null;
	private String user=null;
	private String password=null;
	private String db=null;

	//init LayerManager with database credentials as it requires direct access to the db to create shape files
	//a better way of doing this would be having it receive a result set of geometries -> generate a shape from that, but i never figured out how to do it
	public LayerManager(String ip, String port, String db, String user, String password){
		this.ip=ip;
		this.port=port;
		this.db=db;
		this.user=user;
		this.password=password;
		//create \resouces\temp directory to store the shp files if it isnt already created
		File destinationDir = new File(destination);
		if(!destinationDir.exists()){
			destinationDir.mkdirs();
		}
	}

	//same as above uses URL instead
	//should really break the URL down in the database class so i can use the above class, its more "proper", otherwise, same difference
	public LayerManager(String URL, String user, String password){
		//.splits splits a string into an array of strings on that character, for example
		//"jdbc:postgresql://25.141.219.39:5432/fom"
		//split on "/" becomes jdbc:postgresql:, *note space here due to two //*, 25.141.219.39:5432 and fom
		//split on ":" above for 25.141.219.39:5432 becomes 25.141.219.39 and 5432
		this.ip=URL.split("/")[2].split(":")[0];
		this.port=URL.split("/")[2].split(":")[1];
		this.db=URL.split("/")[3];
		this.user=user;
		this.password=password;
		//create \resouces\temp directory to store the shp files if it isnt already created
		File destinationDir = new File(destination);
		if(!destinationDir.exists()){
			destinationDir.mkdirs();
		}
	}

	//generates a shapefile at fileDestination that encompasses the results that are returned from the query specified in the statement
	//an improvement that can be made here is to include the data inside the shape file also and somehow use that data directly, right now it only returns a shape and a separate query is made for the data
	public File sql2shp(String fileName, String statement){
		String fileDestination=destination+"\\"+fileName+".shp";
		File destination = new File(fileDestination);
		//Check outputs for the inputs going into the proccess builder by uncommenting these
		//System.out.println(statement);
		//System.out.println(exe+"\n" + fileDestination+"\n"+ip+"\n"+port+"\n"+user+"\n"+password+"\n"+db);
		if (destination.exists()){
			return destination;
		}
		Process process;
		try {
			process = new ProcessBuilder(exe, "-f", fileDestination, "-h", ip, "-p", port, "-u", user, "-P", password, db, statement).start();
			/*
			//Check outputs of shape generation by uncommenting this if it all it says is "Initializing..." something is wrong
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
			*/
			process.waitFor();
			if(destination.exists())
				return destination;
			else{
				//System.out.println("Shapefile wasn't generated, check query or simply no results");
				return null;
			}
		} catch (IOException | InterruptedException e) {
			System.out.println(e);
		}
		return null;
	}
}
