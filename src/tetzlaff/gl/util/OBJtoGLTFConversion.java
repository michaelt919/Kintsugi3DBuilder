package tetzlaff.gl.util;

    /*
     * www.javagl.de - JglTF
     *
     * Copyright 2015-2022 Marco Hutter - http://www.javagl.de
     */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.obj.model.ObjGltfModelCreator;

    /**
     * A basic test for the handling of MTL file names that are stored in
     * OBJ files, for the use in the ObjGltfModelCreator
     *
     * Reported at https://github.com/javagl/Obj/issues/20
     */
    public class OBJtoGLTFConversion {
        public static void mainTest(String[] args) throws IOException//was main in the original
        {
            // The 'twoMaterials.obj' does not contain the 'mtllib' keyword.
            // By default, it should read two materials from 'twoMaterials.mtl'.
            runTest("ding");//believe I need to change this to the prefix of the object name
                                            //ex. if I want to convert chameleon.obj, need to put "chameleon" into parameter

            // The 'usingTwoMaterials.obj' refers to 'twoMaterials.mtl'.
            // Before the issue was fixed, this reference was not resolved,
            // and a default material was used. Now, it uses the materials
            // from 'twoMaterials.mtl'
            //runTest("usingTwoMaterials");
        }

        private static void runTest(String baseName) throws IOException //baseName signals the name of the obj file
                                                                        //ex. in chameleon.obj, "chameleon" is the baseName
        {
            System.out.println("Running test for " + baseName);

            //Path basePath = Paths.get("src/test/resources");//this path probably needs to change
            Path basePath = Paths.get("src/tetzlaff/gl/util");//this path might also need to change
            Files.createDirectories(basePath.resolve("output"));

            Path objPath = basePath.resolve(baseName+".obj");
            ObjGltfModelCreator gltfModelCreator = new ObjGltfModelCreator();
            GltfModel gltfModel = gltfModelCreator.create(objPath.toUri());

            List<MaterialModel> materialModels = gltfModel.getMaterialModels();
            System.out.println("Read " + materialModels.size() + " materials");

            GltfModelWriter gltfModelWriter = new GltfModelWriter();
            Path glbPath = basePath.resolve("output/"+baseName+".glb");
            gltfModelWriter.writeBinary(gltfModel, glbPath.toFile());
        }
    }

