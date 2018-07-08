/* Path Normalizer
This algorithm analyses a request path and then maps it to the appropriate endPoint. It is storing all the endPoints in place. The Request Path Component refers to each component in the request path. Every component has a name, an endPoint and an array list of all the child components. As the configurations are input this algorithm creates a n-ary tree for all the configurations. It is also maintaining a wildcardMap to store all the static request patterns (request patterns without any wildcards). This is done to have a faster lookup for a static request pattern and to ensure static paths are given preference over the paths with wildcards. A Queue is also being maintained while mapping the request patterns. The queue contains the componenet containing the wildcard as its children along with its index. This is done in case static paths results in 404 but wildcard path returns the desired endpoint. So before displaying the result as 404, the algorithm checks if the queue is empty. in case it is not empty, all the indexes, where a wildcard was found, are then visited again.

Assumption - System root exists which acts as the root of teh system.

Space Complexity - Since a tree is created from all the request pattern configs
                   O(number of unique components in the request patterns)

Time Complexity - O(1) in case of a static request path ( request path containing no wildcards )
                - O(n) n = components in the request path
*/

import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;
import javafx.util.Pair;

/*
* Data Structure for Request Path Component
*/
class RequestPathComponent{
    /*
    * Name of the component
    */
    private String name;
    /*
    * Children components of the component
    */
    private HashMap<String, RequestPathComponent> childrenMap;
    /*
    * Endpoint string of the component
    */
    private String endPoint;
    
    public RequestPathComponent(String name, HashMap<String, RequestPathComponent> childrenMap, String endPoint){
        this.name = name;
        this.childrenMap = childrenMap;
        this.endPoint = endPoint;
    }  
    
    public String getName(){
        return name;    
    }
    
    public void setName(String name){
        this.name = name;
    }
    
    public HashMap<String, RequestPathComponent> getChildrenMap(){
        return childrenMap;
    }
    
    public void setChildrenMap(HashMap<String,RequestPathComponent> childrenMap){
        this.childrenMap = childrenMap;
    }

    public String getEndPoint(){
        return endPoint;
    }
    
    public void setEndPoint(String endPoint){
        this.endPoint = endPoint;
    }
}

public class Solution {
    public static void main(String args[] ) throws Exception {
        Scanner stdin = new Scanner(new BufferedInputStream(System.in));
        // This acts as the root of the system 
        RequestPathComponent rootComponent = new RequestPathComponent("systemroot", new HashMap<String, RequestPathComponent>(),                     "systemrootEndPoint");
        RequestPathComponent prevComponent = rootComponent;
        // Map to store all the request paths containing no wildcards, this is created to decrease the lookup time for static request paths           and to ensure static request paths are given preference over request paths with wildcards
        HashMap<String,String> requestPathWithoutWildcardsMap = new HashMap<String,String>();
        boolean isConfiguration = true;
        boolean didPreviousNodeChange = true;
        
        while (stdin.hasNextLine()) {
            String str = stdin.nextLine();
            if(str.charAt(0) == '#'){
                isConfiguration = false;
                continue;
            }
            prevComponent = rootComponent;
            // Checks if the input string is the configuration or the request path
            if(isConfiguration){
                createTreeFromRequestPatternConfigs(prevComponent,str,requestPathWithoutWildcardsMap);
            }else{
                handleRequestPaths(prevComponent,str,requestPathWithoutWildcardsMap);
            }
        }
    }
    
    // function to create Tree from the request pattern configs input by the user
    public static void createTreeFromRequestPatternConfigs(RequestPathComponent prevComponent, String config, HashMap<String,String> requestPathWithoutWildcardsMap){
        RequestPathComponent currentComponent = null;
        String[] configsArray = config.split(" ");
        int len = configsArray[0].length();
        String stringWithoutWildcards = configsArray[0].replaceAll("/X","");
        // Checks if the given request path contains any wildcards
        if(len == stringWithoutWildcards.length()){
            // if true, put the request path string and the endpoint in the map
            requestPathWithoutWildcardsMap.put(configsArray[0],configsArray[1]);
            return;
        }
        String[] requestPattern = configsArray[0].replaceAll("^/+", "").split("/");
        for(int i = 0; i < requestPattern.length; i++){
            currentComponent = null;
            // Checks if the previous Request Path component contains the current string in its children map  
            if(prevComponent.getChildrenMap().containsKey(requestPattern[i])){
                prevComponent = prevComponent.getChildrenMap().get(requestPattern[i]);
            }else{
                currentComponent = new RequestPathComponent(requestPattern[i], new HashMap<String,RequestPathComponent>(), ""); 
                prevComponent.getChildrenMap().put(requestPattern[i],currentComponent);
                prevComponent = currentComponent;
            }
        }
        // Sets the endpoint string for the Request Path component
        prevComponent.setEndPoint(configsArray[1]);
    }
    
    // function to handle the request paths and map them to appropriate endpoints 
    public static void handleRequestPaths(RequestPathComponent prevComponent, String requestPath, HashMap<String, String> requestPathWithoutWildcardsMap){
        // Map to store all the Request Path components which contain wildcard/s.
        // Example, Request Patterns - a/b/e/X endPointA
        //                             a/X/c   endPointB
        //          Request Path     - a/b/c
        // This should return endPointA. But since after "a", "b" will get preference over wildcard the tree will choose to go with "b".
        // To handle such cases, this wildcardMap is used, it stores all the wildcards' occurences too and checks this path in case the                  previous path returns 404
        Queue<Pair<RequestPathComponent,Integer>> wildcardMap = new LinkedList<Pair<RequestPathComponent,Integer>>();
        boolean didPreviousNodeChange = true;
        if(requestPathWithoutWildcardsMap.containsKey(requestPath)){
            // Returns the static request paths from the map
            System.out.println(requestPathWithoutWildcardsMap.get(requestPath));
            return;
        }
        String[] requestPathArray = requestPath.replaceAll("^/+", "").split("/");
        if(requestPathArray[0].equals("")){ // For the case when "/" is the request path
            requestPathArray[0] = "/";
        }
        for(int i = 0; i < requestPathArray.length; i++){
            didPreviousNodeChange = true;
            if(prevComponent.getChildrenMap().containsKey(requestPathArray[i])){
                if(prevComponent.getChildrenMap().containsKey("X")){
                    wildcardMap.add(new Pair<RequestPathComponent,Integer>(prevComponent, i-1));
                }
                prevComponent = prevComponent.getChildrenMap().get(requestPathArray[i]);
            }else if(prevComponent.getChildrenMap().containsKey("X")){
                prevComponent = prevComponent.getChildrenMap().get("X");
            }else if(!prevComponent.getName().equals(requestPathArray[i])){
                didPreviousNodeChange = false; /* For the cases when the previous Component does not change and the current component is a                                                     wildcard which does not map to any request path in the configuration.This is to avoid                                                         Configs - /user/X/friends userFriendsEndpoint
                                                  Request Path - /user/123/friends
                                                                /user/123/friends/zzz
                                                  So, based on the configs, the first path should map to friendsEndPoint and the second one                                                     should be a 404.
                                               */
            }
            if(i == requestPathArray.length - 1){
                // if didPreviousNodeChange = false
                if(didPreviousNodeChange && !(prevComponent.getEndPoint().equals(null) || prevComponent.getEndPoint().equals(""))){
                    System.out.println(prevComponent.getEndPoint());
                }else if(wildcardMap.size() == 0){
                    System.out.println("404");
                }else{
                    Pair<RequestPathComponent, Integer> p = wildcardMap.poll();
                    prevComponent = p.getKey().getChildrenMap().get("X");
                    i = p.getValue() + 1;
                }
            }
        }
    }
}