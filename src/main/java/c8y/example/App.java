package c8y.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.model.ID;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;

import c8y.IsDevice;

@MicroserviceApplication
@RestController
public class App{
	
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RequestMapping("hello")
    public String greeting(@RequestParam(value = "name", defaultValue = "world") String name) {
        return "hello " + name + "!";
    }
    
    // You need the inventory API to handle managed objects e.g. creation. You will find this class within the C8Y java client library.
    private final InventoryApi inventoryApi;
    // you need the identity API to handle the external ID e.g. IMEI of a managed object. You will find this class within the C8Y java client library.
    private final IdentityApi identityApi;
    
    @Autowired
    public App(InventoryApi inventoryApi, IdentityApi identityApi) {
        this.inventoryApi = inventoryApi;
        this.identityApi = identityApi;
    }
    
    // http://localhost:8181/create?managedObjectName=WorkhopObject1&managedObjectType=group_temperature_devices
    @RequestMapping("create")
    public String createManagedObject(@RequestParam String managedObjectName, String managedObjectType) {
    	
    	// definition of a new managed object resp. the structure of the object
    	ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();
    	managedObject.setName(managedObjectName);
    	managedObject.setType(managedObjectType);
    	managedObject.set(new IsDevice());
    	
    	// Create the predefined managed object by using the inventory API
    	ManagedObjectRepresentation createdManagedObject = inventoryApi.create(managedObject);
    	
    	// Create the external id of the new device. The external id will be in this case the name of the device.
        ExternalIDRepresentation externalIDRepresentation = new ExternalIDRepresentation();
        externalIDRepresentation.setType("c8y_Serial");
        externalIDRepresentation.setExternalId(managedObjectName);
        externalIDRepresentation.setManagedObject(createdManagedObject);
        identityApi.create(externalIDRepresentation);
        
        return createdManagedObject.toJSON();        
    }
    
    // http://localhost:8181/read?externalId=WorkshopObject1
    @RequestMapping("read")
    public String readManagedObject(@RequestParam String externalId) {
    	// Get the external id representation based on given external id
    	ExternalIDRepresentation externalIDRepresentation = identityApi.getExternalId(new ID("c8y_Serial", externalId));
    	// Get the managed object based on given external id representation
    	ManagedObjectRepresentation managedObjectRepresentation = externalIDRepresentation.getManagedObject();
    	return inventoryApi.get(managedObjectRepresentation.getId()).toJSON();
    }
    
    // http://localhost:8181/update?externalId=WorkshopObject1&newManagedObjectName=WorkshopObjectNameChange
    @RequestMapping("update")
    public String updateManagedObject(@RequestParam String externalId, String newManagedObjectName) {
    	
    	// Create a new managed object and set the id of the managed object you would like to update. 
    	ManagedObjectRepresentation newManagedObjectRepresentation = new ManagedObjectRepresentation();
    	// Because I would like to update the name of my existing managed object I will set new name for my new managed object.
    	newManagedObjectRepresentation.setName(newManagedObjectName);
    	
    	// Get the external id representation of the managed object based on given external id
    	ExternalIDRepresentation externalIDRepresentation = identityApi.getExternalId(new ID("c8y_Serial", externalId));
    	// Get the managed object based on given external id representation
    	ManagedObjectRepresentation managedObjectRepresentation = externalIDRepresentation.getManagedObject();

    	// Update your existing managed object based on given internal id
    	newManagedObjectRepresentation.setId(managedObjectRepresentation.getId());
    	
    	// Update your existing managed object with the new managed object    	
    	inventoryApi.update(newManagedObjectRepresentation);
    	
    	return inventoryApi.get(newManagedObjectRepresentation.getId()).toJSON();
    }
    
    @RequestMapping("delete")
    public String deleteManagedObject(@RequestParam String externalId) {

    	// Get the external id representation of the managed object based on given external id
    	ExternalIDRepresentation externalIDRepresentation = identityApi.getExternalId(new ID("c8y_Serial", externalId));
    	// Get the managed object based on given external id representation
    	ManagedObjectRepresentation managedObjectRepresentation = externalIDRepresentation.getManagedObject();
    	
    	// Delete the managed object
    	inventoryApi.delete(managedObjectRepresentation.getId());
    	
    	return "Managed object has beed deleted";
    }
    
    
}
