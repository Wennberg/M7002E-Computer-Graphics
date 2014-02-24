package assignment4;
 
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

@SuppressWarnings("deprecation")
public class Main extends SimpleApplication implements ActionListener {
	
	private CharacterControl player;
	private Vector3f camera_direction = new Vector3f();
	private Vector3f camera_left = new Vector3f();
	private Vector3f walking_direction = new Vector3f();
	
	private boolean forward = false;
	private boolean left = false;
	private boolean backward = false;
	private boolean right = false;
 
	private BulletAppState bulletAppState;	
	private Node manipulatables;
    
	Material ground_material;
	Material ceiling_material;	
	Material wall_material;
	Material biobox_material;
	Material containmentcontainer_material;
	Material hal9000_material;
	Material door_material;
	
	private RigidBodyControl ground_physical;
	private RigidBodyControl ceiling_physical;
	private RigidBodyControl wall_physical;
	private RigidBodyControl hal9000_physical;
	private RigidBodyControl door_physical;
	
	private static final Box ground;
	private static final Box ceiling;
	private static final Box wall;
	private static final Box hal9000;
	private static final Box door;
	
	static {
		ground = new Box(100f, 0.1f, 100f);
		ground.scaleTextureCoordinates(new Vector2f(6, 6));
    	
		ceiling = new Box(100f, 0.1f, 100f);
		ceiling.scaleTextureCoordinates(new Vector2f(6, 6));
    	
		wall = new Box(0.1f, 100f, 100f);
		wall.scaleTextureCoordinates(new Vector2f(6, 6));
    	
		hal9000 = new Box(0.5f, 6f, 2f);
		hal9000.scaleTextureCoordinates(new Vector2f(3, 6));
    	
		door = new Box(0.5f, 5.3f, 4.5f);
		door.scaleTextureCoordinates(new Vector2f(3, 6));
	}
	
	public static void main(String args[]) {
		Main app = new Main();
		app.start();
  	}
	
	@Override
	public void simpleInitApp() {
		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);
		
		// Change near to stop clipping some objects on contact
		cam.setFrustumPerspective(45f, (float)cam.getWidth()/cam.getHeight(), 0.01f, 1000f);
		
		// Make camera to look at scene 
		cam.setLocation(new Vector3f(0, 4f, 6f));
		cam.lookAt(new Vector3f(2, 2, 0), Vector3f.UNIT_Y);		
		
		// Setting gravity
		bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.82f*5, 0));
		
		// A node to contain all objects that can and should be manipulated in some way
		manipulatables = new Node("Manipulatables");
		rootNode.attachChild(manipulatables);
		
		// Initializing the world and all control and so forth and so on forever and forever
		initKeys();
		initMaterials();
		initGround();
		initCeiling();
		initWalls();
		initContainmentContainers();
		initBioBoxes();
		initHal9000();
		initDoor();
		initPlayer();
		initCrossHair();	
	}

	// Initialized the key mapping to controls work
	private void initKeys() {
		// For moving the player
		inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
	    
		// For making the little crates fidget around
		inputManager.addMapping("BioBox1", new KeyTrigger(KeyInput.KEY_1));
		inputManager.addMapping("BioBox2", new KeyTrigger(KeyInput.KEY_2));
		inputManager.addMapping("BioBox3", new KeyTrigger(KeyInput.KEY_3));
	    
		// Adding all the listeners
		inputManager.addListener(this, "Forward");
		inputManager.addListener(this, "Left");
		inputManager.addListener(this, "Backward");
		inputManager.addListener(this, "Right"); 
		inputManager.addListener(this, "Jump");
		inputManager.addListener(this, "BioBox1");
		inputManager.addListener(this, "BioBox2");
		inputManager.addListener(this, "BioBox3");
	}
	
	@Override
	public void simpleUpdate(float tpf) {
		// Setting the camera
		camera_direction.set(cam.getDirection()).multLocal(0.6f);
		camera_left.set(cam.getLeft()).multLocal(0.4f);
		walking_direction.set(0, 0, 0); 
		
		if (left == true) {
			walking_direction.addLocal(camera_left);
		}
		if (right == true) {
			walking_direction.addLocal(camera_left.negate());
		}
		if (forward == true) {
			walking_direction.addLocal(camera_direction);
		}
		if (backward == true) {
			walking_direction.addLocal(camera_direction.negate());
		}
		
		player.setWalkDirection(walking_direction);
		cam.setLocation(player.getPhysicsLocation());
	}
	
	// Actions performed when button is pressed
	public void onAction(String key_binding, boolean is_pressed, float tpf) {
		if (key_binding.equals("Forward")) {
				forward = is_pressed;
		} else if (key_binding.equals("Left")) {
			left = is_pressed;
		} else if (key_binding.equals("Backward")) {
			backward = is_pressed;
		} else if (key_binding.equals("Right")) {
			right = is_pressed;
		} else if (key_binding.equals("Jump")) {
			if (is_pressed == true) { 
				player.jump(); 
			}
		} else if (key_binding.equals("BioBox1")) { 
			if (is_pressed == true) {	
				manipulateCrate1(tpf);
			}	    	
		} else if (key_binding.equals("BioBox2")) {
			if (is_pressed == true) {
				manipulateCrate2(tpf);
			}
		} else if (key_binding.equals("BioBox3")) {
			if (is_pressed == true) {
				manipulateCrate3(tpf);
			}
		} 
	}
	
	// Making a crate do stuff
	public void manipulateCrate1(float tpf) {
		Spatial spatial = manipulatables.getChild("BioBox1");
		spatial.getControl(RigidBodyControl.class).applyCentralForce(new Vector3f(0f, 10000f, 0f));
	}
	
	public void manipulateCrate2(float tpf) {
		Spatial spatial = manipulatables.getChild("BioBox2");
		spatial.getControl(RigidBodyControl.class).applyCentralForce(new Vector3f(0f, 10000f, 0f));
		spatial.getControl(RigidBodyControl.class).applyTorque(new Vector3f(0f, 4000f, 0f));
	}
	public void manipulateCrate3(float tpf) {
		Spatial spatial = manipulatables.getChild("BioBox3");
		spatial.getControl(RigidBodyControl.class).applyTorque(new Vector3f(0f, 20000f, 0f));
	}
 
	// Materials used in the scene
	public void initMaterials() { 
		ceiling_material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey ceiling_key = new TextureKey("black_tile.png");
		ceiling_key.setGenerateMips(true);
		Texture ceiling_texture = assetManager.loadTexture(ceiling_key);
		ceiling_texture.setWrap(WrapMode.Repeat);
		ceiling_material.setTexture("ColorMap", ceiling_texture);
		
		wall_material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey wall_key = new TextureKey("black_tile.png");
		wall_key.setGenerateMips(true);
		Texture wall_texture = assetManager.loadTexture(wall_key);
		wall_texture.setWrap(WrapMode.Repeat);
		wall_material.setTexture("ColorMap", wall_texture);
		
		ground_material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey ground_key = new TextureKey("black_tile.png");
		ground_key.setGenerateMips(true);
		Texture ground_texture = assetManager.loadTexture(ground_key);
		ground_texture.setWrap(WrapMode.Repeat);
		ground_material.setTexture("ColorMap", ground_texture);
	
		hal9000_material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey hal9000_key = new TextureKey("HAL9000.jpg");
		hal9000_key.setGenerateMips(true);
		Texture hal9000_texture = assetManager.loadTexture(hal9000_key);
		hal9000_material.setTexture("ColorMap", hal9000_texture);
	    
		door_material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey door_key = new TextureKey("dark_steel_door.jpg");
		door_key.setGenerateMips(true);
		Texture door_texture = assetManager.loadTexture(door_key);
		door_material.setTexture("ColorMap", door_texture);
	}
	
	// Make the player
	public void initPlayer() {
		player = new CharacterControl(new CapsuleCollisionShape(1.5f, 6f), 0.05f);
		player.setJumpSpeed(20); 	
		player.setFallSpeed(30); 	
		player.setGravity(80); 		
		player.setPhysicsLocation(new Vector3f(0, 10, 0));
		bulletAppState.getPhysicsSpace().add(player);
	}
 
	// Make solid ground and add it to scene
	public void initGround() {
		rootNode.attachChild(makeGround());
	}
	
	private Geometry makeGround() {
		Geometry ground_geometry = new Geometry("Ground", ground);
		ground_geometry.setMaterial(ground_material);
		ground_geometry.setLocalTranslation(0, 0, 0);
		this.rootNode.attachChild(ground_geometry);
		
		// Creates the ground physical with a mass 0.0f
		ground_physical = new RigidBodyControl(0.0f);
		ground_geometry.addControl(ground_physical);
		bulletAppState.getPhysicsSpace().add(ground_physical);	
		return ground_geometry;
	}
	
	// Make solid ceiling and add it to scene
	public void initCeiling() {
		rootNode.attachChild(makeCeiling());
	}
	
	private Geometry makeCeiling() {
		Geometry ceiling_geometry = new Geometry("Ceiling", ceiling);
		ceiling_geometry.setMaterial(ceiling_material);
		ceiling_geometry.setLocalTranslation(0, 60f, 0);
		this.rootNode.attachChild(ceiling_geometry);
		
		// Creates the ground physical with a mass 0.0f
		ceiling_physical = new RigidBodyControl(0.0f);
		ceiling_geometry.addControl(ceiling_physical);
		bulletAppState.getPhysicsSpace().add(ceiling_physical);
		return ceiling_geometry;
	}
	
	// Making all walls
	public void initWalls() {
		rootNode.attachChild(makeWall(0, 30, 100, 2, 0, 1, 0));
		rootNode.attachChild(makeWall(0, 30, -100, 2, 0, 1, 0));
		rootNode.attachChild(makeWall(100, 30, 0, 2, 0, 0, 0));
		rootNode.attachChild(makeWall(-100, 30, 0, 2, 0, 0, 0));
	}
	
	// Making a single wall
	private Geometry makeWall(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z) {
		Geometry wall_geometry = new Geometry("Wall", wall);
		wall_geometry.setMaterial(wall_material);
		
		// Translating the wall to its location
		wall_geometry.setLocalTranslation(trans_x, trans_y, trans_z);
		
		// Using a quaternion to save a rotation to be used on the wall
		Quaternion rotate90 = new Quaternion(); 
		rotate90.fromAngleAxis(FastMath.PI/rad, new Vector3f(rot_x, rot_y, rot_z));  
		wall_geometry.setLocalRotation(rotate90);
		
		// Creates the wall physical with a mass 0.0f
		wall_physical = new RigidBodyControl(0.0f);
		wall_geometry.addControl(wall_physical);
		bulletAppState.getPhysicsSpace().add(wall_physical);
		return wall_geometry;
	}
	
	// Making all ContainmentContainers
	public void initContainmentContainers() {
		// Large corner crates
		rootNode.attachChild(makeContainmentContainer(50, 15, 50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer1"));
		rootNode.attachChild(makeContainmentContainer(-50, 15, 50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer2"));
		rootNode.attachChild(makeContainmentContainer(50, 15, -50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer3"));	
		rootNode.attachChild(makeContainmentContainer(-50, 15, -50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer4"));
	}
	
	// Making a single ContainmentContainer
	private Geometry makeContainmentContainer(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale, float mass, String name) {
		Box cube = new Box(1f, 1f, 1f);
		Geometry cube_geometry = new Geometry(name, cube);
		cube_geometry.setLocalTranslation(new Vector3f(trans_x, trans_y, trans_z));
		Material cube_material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		Texture cube_texture = assetManager.loadTexture("containmentcontainer.png");
		cube_material.setTexture("ColorMap", cube_texture);
	    
		// Using a quaternion to save a rotation to be used on the wall
		Quaternion rotate90 = new Quaternion(); 
		rotate90.fromAngleAxis(FastMath.PI/rad, new Vector3f(rot_x, rot_y, rot_z));
		cube_geometry.setLocalRotation(rotate90);
	 
		cube_geometry.setLocalScale(scale);
		cube_geometry.setMaterial(cube_material);
	    
		// Adding a collision box to geometry
		CollisionShape cube_shape = CollisionShapeFactory.createBoxShape(cube_geometry);
		RigidBodyControl cube_physical = new RigidBodyControl(cube_shape, mass);
	    	    
		cube_physical.setFriction(5f);
		cube_geometry.addControl(cube_physical);
		bulletAppState.getPhysicsSpace().add(cube_physical);
		return cube_geometry;
	}
	
	// Making the BioBoxes
	public void initBioBoxes() {
		// Small crates
		manipulatables.attachChild(makeBioBox(-75f, 1.5f, 10f, 2, 0, 0, 0, 1.5f, 9f, "BioBox1"));
		manipulatables.attachChild(makeBioBox(-75f, 1.5f, 0, 2, 0, 0, 0, 1.5f, 9f, "BioBox2"));	
		manipulatables.attachChild(makeBioBox(-75f, 1.5f, -10f, 2, 0, 0, 0, 1.5f, 9f, "BioBox3"));
	}
	
	// Making a single BioBox
	private Geometry makeBioBox(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale, float mass, String name) {
		Box cube = new Box(1f, 1f, 1f);
		Geometry cube_geometry = new Geometry(name, cube);
		cube_geometry.setLocalTranslation(new Vector3f(trans_x, trans_y, trans_z));
		Material cube_material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		Texture cube_texture = assetManager.loadTexture("biohazard.png");
		cube_material.setTexture("ColorMap", cube_texture);
	    
		// Using a quaternion to save a rotation to be used on the wall
		Quaternion rotate90 = new Quaternion(); 
		rotate90.fromAngleAxis(FastMath.PI/rad, new Vector3f(rot_x, rot_y, rot_z));
		cube_geometry.setLocalRotation(rotate90);
	 
		cube_geometry.setLocalScale(scale);
		cube_geometry.setMaterial(cube_material);
	    
		// Adding a collision box to geometry
		CollisionShape cube_shape = CollisionShapeFactory.createBoxShape(cube_geometry);
		RigidBodyControl cube_physical = new RigidBodyControl(cube_shape, mass);
	    	    
		cube_physical.setFriction(5f);
		cube_geometry.addControl(cube_physical);
		bulletAppState.getPhysicsSpace().add(cube_physical);
		return cube_geometry;
	}
	
	// Making all HAL9000s
	public void initHal9000(){
		rootNode.attachChild(makeHal9000(-101.6f, 30, 0, 2, 0, 0, 0, 4));
	}
	
	// Make a single HAL9000
	private Geometry makeHal9000(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale) {
		Geometry hal9000_geometry = new Geometry("HAL9000", hal9000);
		hal9000_geometry.setMaterial(hal9000_material);
		
		// Translating to its location
		hal9000_geometry.setLocalTranslation(trans_x, trans_y, trans_z);
		hal9000_geometry.setLocalScale(scale);
		
		// Using a quaternion to save a rotation to be used
		Quaternion rotate90 = new Quaternion(); 
		rotate90.fromAngleAxis(FastMath.PI/rad, new Vector3f(rot_x, rot_y, rot_z));  
		hal9000_geometry.setLocalRotation(rotate90);
		
		// Scales the textures
		hal9000_geometry.getMesh().scaleTextureCoordinates(new Vector2f(0.335f, 0.166f));
		
		// Creates the physical with mass as argument
		hal9000_physical = new RigidBodyControl(0f);
		hal9000_geometry.addControl(hal9000_physical);
		bulletAppState.getPhysicsSpace().add(hal9000_physical);
		return hal9000_geometry;
	}
	
	// Making all doors
	public void initDoor(){
		rootNode.attachChild((makeDoor(8, 6, 100.4f, 2, 0, 1, 0, 1.2f)));
	}
	
	// Making a single door
	private Geometry makeDoor(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale) {
		Geometry door_geometry = new Geometry("Door", door);
		door_geometry.setMaterial(door_material);
		
		// Translating to its location
		door_geometry.setLocalTranslation(trans_x, trans_y, trans_z);
		door_geometry.setLocalScale(scale);
		
		// Using a quaternion to save a rotation to be used
		Quaternion rotate90 = new Quaternion(); 
		rotate90.fromAngleAxis(FastMath.PI/rad, new Vector3f(rot_x, rot_y, rot_z));  
		door_geometry.setLocalRotation(rotate90);
		
		// Experimented with these values and this gave good resutls
		door_geometry.getMesh().scaleTextureCoordinates(new Vector2f(0.335f, 0.166f));
		
		// Creates the physical with mass as argument
		door_physical = new RigidBodyControl(0f);
		door_geometry.addControl(door_physical);
		bulletAppState.getPhysicsSpace().add(door_physical);
		return door_geometry;
	}
	
	// Crosshairs
	protected void initCrossHair() {
		guiNode.detachAllChildren();
		guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
		BitmapText crosshairs = new BitmapText(guiFont, false);
		
		crosshairs.setSize(guiFont.getCharSet().getRenderedSize()*2);
		crosshairs.setText("+");        
		crosshairs.setLocalTranslation(settings.getWidth()/2 - guiFont.getCharSet().getRenderedSize()/(3*2), settings.getHeight()/2 + crosshairs.getLineHeight()/2, 0);
		guiNode.attachChild(crosshairs);
	}
}