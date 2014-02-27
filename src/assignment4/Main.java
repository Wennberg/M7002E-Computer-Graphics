package assignment4;
 
import java.util.UUID;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.ui.Picture;

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
    private Node inventory;
    private Node announcer;
    
    private Picture hal_mode;
    
    private int camera_position = 0;
    private Vector3f last_player_camera_direction;
    //private Vector3f last_player_camera_location;
    private Vector3f last_hal_camera_direction = new Vector3f(15f,  0f, 0f);
    
    
    //private Vector3f last_position;
    private Vector3f last_scale;
    private RigidBodyControl last_physical;
	
	Material ground_material;
	Material ceiling_material;	
	Material wall_material;
	Material biobox_material;
	Material containmentcontainer_material;
	Material hal9000_material;
	Material door_material;
	Material lamp_material;
	
	private RigidBodyControl ground_physical;
	private RigidBodyControl ceiling_physical;
	private RigidBodyControl wall_physical;
	private RigidBodyControl hal9000_physical;
	private RigidBodyControl door_physical;
	private RigidBodyControl lamp_physical;

	private static final Box ground;
	private static final Box ceiling;
	private static final Box wall;
	private static final Box hal9000;
	private static final Box door;
	private static final Box lamp;
	
	private static final Cylinder creator;
	private static final Cylinder destroyer;
	
	private Spatial selected_object;
	
	static {
		ground = new Box(100f, 2f, 100f);
		ground.scaleTextureCoordinates(new Vector2f(6, 6));
    	
		ceiling = new Box(100f, 2f, 100f);
		ceiling.scaleTextureCoordinates(new Vector2f(6, 6));
    	
		wall = new Box(2f, 100f, 100f);
		wall.scaleTextureCoordinates(new Vector2f(6, 6));
    	
		hal9000 = new Box(0.5f, 6f, 2f);
		hal9000.scaleTextureCoordinates(new Vector2f(3, 6));
		
		lamp = new Box(3f, 0.1f, 3f);
		lamp.scaleTextureCoordinates(new Vector2f(1, 1));
    	
		door = new Box(0.5f, 5.3f, 4.5f);
		door.scaleTextureCoordinates(new Vector2f(3, 6));
		
		creator = new Cylinder(10, 50, 0.1f, 3f, true);
		creator.scaleTextureCoordinates(new Vector2f(0.1f, 1f));
		
		destroyer = new Cylinder(10, 50, 0.1f, 3f, true);
		destroyer.scaleTextureCoordinates(new Vector2f(0.1f, 1f));
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
		cam.lookAt(new Vector3f(-100, 15, 7), Vector3f.UNIT_Y);		
	
		rootNode.setShadowMode(ShadowMode.CastAndReceive);
		
		// Setting gravity
		bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.82f*5, 0));
		
		// A node to contain all objects that can and should be manipulated in some way
		manipulatables = new Node("Manipulatables");
		announcer = new Node("Announcer");
		
		inventory = new Node("Inventory");
		guiNode.attachChild(inventory);
		guiNode.attachChild(announcer);
		
		rootNode.attachChild(manipulatables);	
		
		// Initializing the world and all control and so forth and so on forever and forever
		initKeys();
		initMaterials();
		initGround();
		initCeiling();
		initWalls();
		initLight();
		initContainmentContainers();
		initBioBoxes();
		initHal9000();
		initDoor();
		initLamp();
		initPlayer();
		initCrossHair();
		initHALMode();
		initCreator();
		initDestroyer();		
	}
	
	public void initLight() {
		// Adding a light to the HUD
		DirectionalLight hud_light = new DirectionalLight();
		hud_light.setDirection(new Vector3f(0, 0, -1.0f));
		guiNode.addLight(hud_light);
		
		// Adding a light from the ceiling
		PointLight light_ceiling = new PointLight();
		light_ceiling.setColor(ColorRGBA.White);
		light_ceiling.setRadius(600f);
		light_ceiling.setPosition(new Vector3f(0, 50, 0));
		rootNode.addLight(light_ceiling);
		
		// Adding a point light from HAL
		PointLight light_hal = new PointLight();
		light_hal.setColor(ColorRGBA.White);
		light_hal.setRadius(6000f);
		light_hal.setPosition(new Vector3f(-95, 20, 0));
		rootNode.addLight(light_hal);
		
		
		// Shadows
		//TODO: Fix so can have multiple light sources with shadows
		final int SHADOWMAP_SIZE = 512;
		
		PointLightShadowRenderer plsr_ceiling = new PointLightShadowRenderer(assetManager, SHADOWMAP_SIZE);
		plsr_ceiling.setLight(light_ceiling);
        viewPort.addProcessor(plsr_ceiling);
        
        PointLightShadowRenderer plsr_hal = new PointLightShadowRenderer(assetManager, SHADOWMAP_SIZE);
        plsr_hal.setLight(light_hal);
        viewPort.addProcessor(plsr_hal);
		
	}

	// Initialized the key mapping to controls work
	private void initKeys() {
		// For moving the player
		inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
	    
		inputManager.addMapping("Use", new KeyTrigger(KeyInput.KEY_E));
		inputManager.addMapping("Throw", new KeyTrigger(KeyInput.KEY_T));
		inputManager.addMapping("Pick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		inputManager.addMapping("Drop", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
		
		inputManager.addMapping("Switch", new KeyTrigger(KeyInput.KEY_TAB));
	   
		inputManager.addListener(this, "Forward");
		inputManager.addListener(this, "Left");
		inputManager.addListener(this, "Backward");
		inputManager.addListener(this, "Right"); 
		inputManager.addListener(this, "Jump");
		
		inputManager.addListener(this, "Throw");
		inputManager.addListener(this, "Use");
		inputManager.addListener(this, "Drop");
		inputManager.addListener(this, "Pick");
		
		inputManager.addListener(this, "Switch");
		
		//TODO: DOES NOT WORK
		// Disabling the mouse wheel scroll zoom
		inputManager.deleteMapping("FLYCAM_ZoomIn");
		inputManager.deleteMapping("FLYCAM_ZoomOut");
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
		
		if (camera_position == 0) {
			cam.setLocation(player.getPhysicsLocation());
		} else if (camera_position == 1) {
			cam.setLocation(new Vector3f(-97f, 26.5f, 0));
			
			//TODO: Limit camera rotation
			
			
			// ALMOST working, but not really
			/*if (cam.getUp().y < 0) {
				cam.lookAtDirection(new Vector3f(0, cam.getDirection().y, 0), new Vector3f(cam.getUp().x, 0, cam.getUp().z));
			} 
			 */
			
			
			// More like it, but not really. 
			/*if (cam.getDirection().y > FastMath.QUARTER_PI || cam.getDirection().y < -FastMath.QUARTER_PI) { 
				
				//Vector3f cam_dir = cam.getDirection();
				//Vector3f camera = new Vector3f(cam_dir.getX(), FastMath.QUARTER_PI, cam_dir.getZ());
				cam.lookAtDirection(new Vector3f(0, cam.getDirection().y, 0), new Vector3f(cam.getDirection().x, 0, cam.getDirection().z));
			}*/
		}
		
		// Handling the text announcements
		if (!(selected_object == null)) {
			guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
			BitmapText text = new BitmapText(guiFont, false);
			text.setSize(guiFont.getCharSet().getRenderedSize()*2);
			
			if (selected_object.getName().equals("Destroyer")) {
				text.setText("Wielding the Destroyer!");        
			} else if (selected_object.getName().equals("Creator")) {
				text.setText("Wielding the Creator!");      
			} else {
				text.setText("");
			}
			text.setLocalTranslation(settings.getWidth()/38 - guiFont.getCharSet().getRenderedSize()/(3*2), settings.getHeight()/12 + text.getLineHeight()/6, 0);
			announcer.attachChild(text);
		}
	}
	
	// Actions performed when button is pressed
	public void onAction(String key_binding, boolean is_pressed, float tpf) {
		if (key_binding.equals("Switch")) {		
			if (is_pressed == true) {
				if (camera_position == 0) {
					// Switching to HAL9000 mode
					last_player_camera_direction = cam.getDirection();
					//last_player_camera_location = cam.getLocation();
					
					guiNode.attachChild(hal_mode);
					
					cam.lookAtDirection(last_hal_camera_direction, Vector3f.UNIT_Y);
					camera_position = 1;
					
				} else if (camera_position == 1) {
					// Switching to player mode
					guiNode.detachChild(hal_mode);
					last_hal_camera_direction = cam.getDirection();
					cam.lookAtDirection(last_player_camera_direction, Vector3f.UNIT_Y);
					camera_position = 0;					
				}
			}
		} else if (camera_position == 0) {
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
			} else if (key_binding.equals("Throw")) { 
				if (is_pressed == true) {	
					
					if (selected_object == null) {
						return;
					} else if (inventory.getChildren().isEmpty() == false) {					
						operationDrop();
						operationThrow();
						selected_object = null;
						announcer.detachAllChildren();
					}
				}	    	
			} else if (key_binding.equals("Drop")) { 
				if (is_pressed == true) {	
					
					if (selected_object == null) {
						return;
					} else if (inventory.getChildren().isEmpty() == false) {					
						operationDrop();
						selected_object = null;
						announcer.detachAllChildren();
					}
				}	    	
			} else if (key_binding.equals("Use")) {
				if (is_pressed == true) {
					// Check that you are holding the some item, else do not allow action
					if (inventory.getChildren().isEmpty() == false) {
						if (inventory.getChild(0).getName().equals("Creator") == true) {
							operationCreate(tpf);
						} else if (inventory.getChild(0).getName().equals("Destroyer") == true) {
							operationDestroy(tpf);
						} else {
							return;
						}
					} else {
						return;
					}
				}
			} else if (key_binding.equals("Pick") && camera_position == 0) {
				if (is_pressed == false) {
					// If holding an item and clicking you put it down
					if (inventory.getChildren().isEmpty() == false) {
						operationDrop();
						selected_object = null;
						announcer.detachAllChildren();
					} else {
						CollisionResults collisions = new CollisionResults();
						Ray ray = new Ray(cam.getLocation(), cam.getDirection());
						manipulatables.collideWith(ray, collisions);
						
						if (collisions.size() > 0) {
							CollisionResult closest = collisions.getClosestCollision();
							Spatial spatial = closest.getGeometry();
							last_scale = spatial.getLocalScale().clone();
							//last_position = spatial.getLocalTranslation().clone();
							
							last_physical = spatial.getControl(RigidBodyControl.class);
							bulletAppState.getPhysicsSpace().remove(last_physical);
							spatial.removeControl(RigidBodyControl.class);
							
							manipulatables.detachChild(spatial);
							inventory.attachChild(spatial);
							
							spatial.setLocalScale(150f);
							spatial.setLocalTranslation(settings.getWidth()/2, settings.getHeight()/2, 0);	
							
							selected_object = spatial;
						}  
					}
				}		
			} 
		} else if (camera_position == 1) {
			// What should HAL900 be able to do?
			// Just look pretty?
			
			
			
			
			
			
			
			
			
		} 
	}
	
	public void operationDrop(){
		Spatial spatial = inventory.getChild(0);
		spatial.setLocalScale(last_scale);
		
		Vector3f location = cam.getLocation();
		Vector3f direction = cam.getDirection();
		float trans_x = location.getX() + (7) * direction.getX();
		float trans_y = location.getY() + (7) * direction.getY();
		float trans_z = location.getZ() + (7) * direction.getZ();
		Vector3f new_position = new Vector3f(trans_x, trans_y, trans_z);
		
		last_physical.setPhysicsLocation(new_position);
		spatial.setLocalTranslation(new_position);
		spatial.addControl(last_physical);
		
		bulletAppState.getPhysicsSpace().add(last_physical);
		
		last_physical.activate();
		inventory.detachAllChildren();
		manipulatables.attachChild(spatial);
	}
	
	public void operationThrow() {
		// Handling not selected any object
		if (selected_object == null) {
			return;
		}
		
		String name = selected_object.getName();
		Spatial spatial = manipulatables.getChild(name);
		Vector3f direction = cam.getDirection();
		spatial.getControl(RigidBodyControl.class).applyImpulse(direction.mult(500), new Vector3f(0, 0, 0));
	}
	
	public void operationCreate(float tpf) {
		Vector3f location = cam.getLocation();
		Vector3f direction = cam.getDirection();
		
		float trans_x = location.getX() + (5) * direction.getX();
		float trans_y = location.getY() + (5) * direction.getY();
		float trans_z = location.getZ() + (5) * direction.getZ();
		float rad = 2;
		float rot_x = 0;
		float rot_y = 0;
		float rot_z = 0;
		float scale = 1;
		float mass = 9;
		String name = UUID.randomUUID().toString();
		
		manipulatables.attachChild(makeBioBox(trans_x, trans_y, trans_z, rad, rot_x, rot_y, rot_z, scale, mass, name));
	}
	
	public void operationDestroy(float tpf) {
		CollisionResults collisions = new CollisionResults();
		Ray ray = new Ray(cam.getLocation(), cam.getDirection());
		manipulatables.collideWith(ray, collisions);
		
		if (collisions.size() > 0) {
			CollisionResult closest = collisions.getClosestCollision();
			Spatial spatial = closest.getGeometry();
			bulletAppState.getPhysicsSpace().remove(spatial.getControl(RigidBodyControl.class));
			spatial.removeControl(RigidBodyControl.class);
			manipulatables.detachChild(spatial);
		}  
	}
 
	// Materials used in the scene
	public void initMaterials() { 
		ceiling_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		TextureKey ceiling_key = new TextureKey("black_tile.png");
		ceiling_key.setGenerateMips(true);
		Texture ceiling_texture = assetManager.loadTexture(ceiling_key);
		ceiling_texture.setWrap(WrapMode.Repeat);
		ceiling_material.setTexture("DiffuseMap", ceiling_texture);
		
		wall_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		TextureKey wall_key = new TextureKey("black_tile.png");
		wall_key.setGenerateMips(true);
		Texture wall_texture = assetManager.loadTexture(wall_key);
		wall_texture.setWrap(WrapMode.Repeat);
		wall_material.setTexture("DiffuseMap", wall_texture);
		
		ground_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		TextureKey ground_key = new TextureKey("black_tile.png");
		ground_key.setGenerateMips(true);
		Texture ground_texture = assetManager.loadTexture(ground_key);
		ground_texture.setWrap(WrapMode.Repeat);
		ground_material.setTexture("DiffuseMap", ground_texture);
		
		lamp_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		TextureKey lamp_key = new TextureKey("glow.jpg");
		lamp_key.setGenerateMips(true);
		Texture lamp_texture = assetManager.loadTexture(lamp_key);
		//lamp_texture.setWrap(WrapMode.Repeat);
		lamp_material.setTexture("DiffuseMap", lamp_texture);
	
		hal9000_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		TextureKey hal9000_key = new TextureKey("HAL9000.jpg");
		hal9000_key.setGenerateMips(true);
		Texture hal9000_texture = assetManager.loadTexture(hal9000_key);
		hal9000_material.setTexture("DiffuseMap", hal9000_texture);
	    
		door_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		TextureKey door_key = new TextureKey("dark_steel_door.jpg");
		door_key.setGenerateMips(true);
		Texture door_texture = assetManager.loadTexture(door_key);
		door_material.setTexture("DiffuseMap", door_texture);
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
		ground_geometry.setLocalTranslation(0, -2, 0);
		this.rootNode.attachChild(ground_geometry);
		
		ground_geometry.setShadowMode(ShadowMode.Receive);
	
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
		ceiling_geometry.setLocalTranslation(0, 62f, 0);
		this.rootNode.attachChild(ceiling_geometry);
		
		ceiling_geometry.setShadowMode(ShadowMode.Receive);
		
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
		
		wall_geometry.setShadowMode(ShadowMode.Receive);
		
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
		rootNode.attachChild(makeContainmentContainer(50, 10.1f, 50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer1"));
		rootNode.attachChild(makeContainmentContainer(50, 30.1f, 50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer2"));
		rootNode.attachChild(makeContainmentContainer(29, 10.1f, 50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer3"));		
		rootNode.attachChild(makeContainmentContainer(50, 10.1f, 29, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer4"));
	
		rootNode.attachChild(makeContainmentContainer(-50, 30.1f, 50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer5"));
		rootNode.attachChild(makeContainmentContainer(-50, 10.1f, 50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer6"));
		
		rootNode.attachChild(makeContainmentContainer(29, 10.1f, -50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer7"));
		rootNode.attachChild(makeContainmentContainer(50, 10.1f, -50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer8"));	
		rootNode.attachChild(makeContainmentContainer(50, 30.1f, -50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer9"));	
		rootNode.attachChild(makeContainmentContainer(50, 10.1f, -29, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer10"));	
		
		rootNode.attachChild(makeContainmentContainer(-50, 30.1f, -50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer11"));
		rootNode.attachChild(makeContainmentContainer(-50, 10.1f, -50, 2, 0, 0, 0, 10, 1000f, "ContainmentContainer12"));
	}
	
	// Making a single ContainmentContainer
	private Geometry makeContainmentContainer(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale, float mass, String name) {
		Box cube = new Box(1f, 1f, 1f);
		Geometry cube_geometry = new Geometry(name, cube);
		cube_geometry.setLocalTranslation(new Vector3f(trans_x, trans_y, trans_z));
		Material cube_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		Texture cube_texture = assetManager.loadTexture("containmentcontainer.png");
		//Texture cube_texture = assetManager.loadTexture("crate.jpg");
		cube_material.setTexture("DiffuseMap", cube_texture);
	    
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
	
	public void initCreator() {
		manipulatables.attachChild(makeCreator());
	}
	
	private Geometry makeCreator() {
		Geometry creator_geometry = new Geometry("Creator", creator);
		creator_geometry.setLocalTranslation(new Vector3f(-50, 2, 5f));
		Material creator_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		Texture creator_texture = assetManager.loadTexture("rod_green.jpg");
		creator_material.setTexture("DiffuseMap", creator_texture);
	    
		// Using a quaternion to save a rotation to be used on the wall
		Quaternion rotate90 = new Quaternion(); 
		rotate90.fromAngleAxis(FastMath.PI/2, new Vector3f(0, 0, 0));
		creator_geometry.setLocalRotation(rotate90);
	 
		creator_geometry.setLocalScale(1f);
		creator_geometry.setMaterial(creator_material);
	    
		// Adding a collision box to geometry
		CollisionShape creator_shape = CollisionShapeFactory.createBoxShape(creator_geometry);
		RigidBodyControl creator_physical = new RigidBodyControl(creator_shape, 10f);
	    	    
		creator_physical.setFriction(5f);
		creator_geometry.addControl(creator_physical);
		bulletAppState.getPhysicsSpace().add(creator_physical);
		return creator_geometry;
	}
	
	public void initDestroyer() {
		manipulatables.attachChild(makeDestroyer());
	}
	
	private Geometry makeDestroyer() {
		Geometry destroyer_geometry = new Geometry("Destroyer", destroyer);
		destroyer_geometry.setLocalTranslation(new Vector3f(-50, 2, -5f));
		Material destroyer_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		Texture destroyer_texture = assetManager.loadTexture("rod_red.jpg");
		destroyer_material.setTexture("DiffuseMap", destroyer_texture);
		
		// Using a quaternion to save a rotation to be used on the wall
		Quaternion rotate90 = new Quaternion(); 
		rotate90.fromAngleAxis(FastMath.PI/2, new Vector3f(0, 0, 0));
		destroyer_geometry.setLocalRotation(rotate90);
	 
		destroyer_geometry.setLocalScale(1f);
		destroyer_geometry.setMaterial(destroyer_material);
	    
		// Adding a collision box to geometry
		CollisionShape destroyer_shape = CollisionShapeFactory.createBoxShape(destroyer_geometry);
		RigidBodyControl destroyer_physical = new RigidBodyControl(destroyer_shape, 10f);
	    	    
		destroyer_physical.setFriction(5f);
		destroyer_geometry.addControl(destroyer_physical);
		bulletAppState.getPhysicsSpace().add(destroyer_physical);
		return destroyer_geometry;
	}
	
	// Making the BioBoxes
	public void initBioBoxes() {
		// BioBoxes
		manipulatables.attachChild(makeBioBox(-75f, 1.5f, 10f, 2, 0, 0, 0, 1.5f, 9f, "BioBox1"));
		manipulatables.attachChild(makeBioBox(-75f, 1.5f, 0, 2, 0, 0, 0, 1.5f, 9f, "BioBox2"));	
		manipulatables.attachChild(makeBioBox(-75f, 1.5f, -10f, 2, 0, 0, 0, 1.5f, 9f, "BioBox3"));
	}
	
	// Making a single BioBox
	private Geometry makeBioBox(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale, float mass, String name) {
		Box cube = new Box(1f, 1f, 1f);
		Geometry cube_geometry = new Geometry(name, cube);
		cube_geometry.setLocalTranslation(new Vector3f(trans_x, trans_y, trans_z));
		Material cube_material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		Texture cube_texture = assetManager.loadTexture("biohazard.png");
		cube_material.setTexture("DiffuseMap", cube_texture);
		
		//cube_material.setColor("Diffuse", ColorRGBA.White);
		//cube_material.setColor("Specular", ColorRGBA.White);
		//cube_material.setBoolean("UseMaterialColors", true);
		
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
		rootNode.attachChild(makeHal9000(-99.6f, 30, 0, 2, 0, 0, 0, 4));
	}
	
	// Make a single HAL9000
	private Geometry makeHal9000(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale) {
		Geometry hal9000_geometry = new Geometry("HAL9000", hal9000);
		hal9000_geometry.setMaterial(hal9000_material);
		
		// Translating to its location
		hal9000_geometry.setLocalTranslation(trans_x, trans_y, trans_z);
		hal9000_geometry.setLocalScale(scale);
		
		hal9000_geometry.setShadowMode(ShadowMode.Receive);
		
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
		rootNode.attachChild((makeDoor(8, 6, 98.4f, 2, 0, 1, 0, 1.2f)));
	}
	
	// Making a single door
	private Geometry makeDoor(float trans_x, float trans_y, float trans_z, float rad, float rot_x, float rot_y, float rot_z, float scale) {
		Geometry door_geometry = new Geometry("Door", door);
		door_geometry.setMaterial(door_material);
		
		// Translating to its location
		door_geometry.setLocalTranslation(trans_x, trans_y, trans_z);
		door_geometry.setLocalScale(scale);
		
		door_geometry.setShadowMode(ShadowMode.Receive);
		
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
	
	public void initLamp() {
		rootNode.attachChild(makeLamp());
	}
	
	private Geometry makeLamp() {
		Geometry lamp_geometry = new Geometry("Lamp", lamp);
		lamp_geometry.setMaterial(lamp_material);
		lamp_geometry.setLocalTranslation(0, 60, 0);
				
		lamp_geometry.setShadowMode(ShadowMode.Cast);
	
		lamp_physical = new RigidBodyControl(0.0f);
		lamp_geometry.addControl(lamp_physical);
		bulletAppState.getPhysicsSpace().add(lamp_physical);	
		return lamp_geometry;
	}
	
	public void initHALMode() {
		hal_mode = new Picture("HAL9000 Mode");
		hal_mode.setImage(assetManager, "hal_mode_filter.png", true);
		hal_mode.setHeight(settings.getHeight());
		hal_mode.setWidth(settings.getWidth());	
	}
	
	// Crosshairs
	protected void initCrossHair() {
		// Hiding stat box
		setDisplayStatView(false);
		
		//guiNode.detachAllChildren();
		guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
		BitmapText crosshairs = new BitmapText(guiFont, false);
		
		crosshairs.setSize(guiFont.getCharSet().getRenderedSize()*2);
		crosshairs.setText("+");        
		crosshairs.setLocalTranslation(settings.getWidth()/2 - guiFont.getCharSet().getRenderedSize()/(3*2), settings.getHeight()/2 + crosshairs.getLineHeight()/2, 0);
		guiNode.attachChild(crosshairs);
	}
}