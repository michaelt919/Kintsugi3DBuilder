import os
import sys
import bpy
from pxr import Sdf, Usd, UsdShade, UsdGeom


def find_mesh(_stage):
        print('Finding mesh...')
        for p in stage.Traverse():
                if p.IsA(UsdGeom.Mesh):
                        print(f'Binding material to {p.GetPath()}')
                        return p

        print('Could not bind mesh')
        return None


def gen_uv_texture(_stage, _material_path, _resource_path, _uv, _id):
        print(f'Linking {_id} texture...')
        _uv_texture = UsdShade.Shader.Define(_stage, _material_path.AppendChild(_id))
        _uv_texture.CreateIdAttr('UsdUVTexture')
        _uv_texture.CreateInput('file', Sdf.ValueTypeNames.Asset).Set(_resource_path)
        _uv_texture.CreateInput('st', Sdf.ValueTypeNames.Float2).ConnectToSource(_uv.ConnectableAPI(), 'result')
        return _uv_texture


def gen_normal(_stage, _material_path, _uv, _texture_format):
        _normal = gen_uv_texture(_stage, _material_path, f'textures/normal.{_texture_format}', _uv, 'normal')
        _normal.CreateOutput('rgb', Sdf.ValueTypeNames.Float3)
        _normal.CreateInput('wrapS', Sdf.ValueTypeNames.Token).Set('black')
        _normal.CreateInput('wrapT', Sdf.ValueTypeNames.Token).Set('clamp')
        return _normal


def gen_diffuse(_stage, _material_path, _uv, _texture_format):
        _diffuse = gen_uv_texture(_stage, _material_path, f'textures/diffuse.{_texture_format}', _uv, 'diffuse')
        _diffuse.CreateOutput('rgb', Sdf.ValueTypeNames.Float3)
        return _diffuse


def gen_specular(_stage, _material_path, _uv, _texture_format):
        _specular = gen_uv_texture(_stage, _material_path, f'textures/specular.{_texture_format}', _uv, 'specular')
        _specular.CreateOutput('rgb', Sdf.ValueTypeNames.Float3)
        return _specular


def gen_roughness(_stage, _material_path, _uv, _texture_format):
        _roughness = gen_uv_texture(_stage, _material_path, f'textures/roughness.{_texture_format}', _uv, 'roughness')
        _roughness.CreateOutput('r', Sdf.ValueTypeNames.Float)
        return _roughness


def gen_st(_stage, _material_path, _material):
        print('Unwraping UV...')
        _st = UsdShade.Shader.Define(_stage, _material_path.AppendChild('st'))
        _st.CreateIdAttr('UsdPrimvarReader_float2')
        _st.CreateInput('varname', Sdf.ValueTypeNames.Token).ConnectToSource(_material.GetInput('frame:stPrimvarName'))
        _st.CreateOutput('result', Sdf.ValueTypeNames.Float2)
        return _st


def gen_output(_stage, _material_path, _normal, _diffuse, _specular, _roughness):
        print('Generating PBR output shader...')
        _output = UsdShade.Shader.Define(_stage, _material_path.AppendChild('pbr'))
        _output.CreateIdAttr("UsdPreviewSurface")
        _output.CreateInput('useSpecularWorkflow', Sdf.ValueTypeNames.Int).Set(1)
        _output.CreateInput("normal", Sdf.ValueTypeNames.Normal3f).ConnectToSource(_normal.ConnectableAPI(), "rgb")
        _output.CreateInput("diffuseColor", Sdf.ValueTypeNames.Color3f).ConnectToSource(_diffuse.ConnectableAPI(), "rgb")
        _output.CreateInput("specularColor", Sdf.ValueTypeNames.Color3f).ConnectToSource(_specular.ConnectableAPI(), "rgb")
        _output.CreateInput("roughness", Sdf.ValueTypeNames.Float).ConnectToSource(_roughness.ConnectableAPI(), "r")
        return _output


def gen_material(_stage, _mesh_path, _texture_format):
        print('Generating material...')
        _material = UsdShade.Material.Define(stage, _mesh_path.AppendChild('mat'))
        _material.CreateInput('frame:stPrimvarName', Sdf.ValueTypeNames.Token).Set('st')
        _material_path = _material.GetPath()

        # Find and attach our UV map to our shader
        _st = gen_st(_stage, _material_path, _material)

        # Create our texture shaders
        _normal = gen_normal(_stage, _material_path, _st, _texture_format)
        _diffuse = gen_diffuse(_stage, _material_path, _st, _texture_format)
        _specular = gen_specular(_stage, _material_path, _st, _texture_format)
        _roughness = gen_roughness(_stage, _material_path, _st, _texture_format)

        # Attach our material inputs to a new output shader
        _output = gen_output(_stage, _material_path, _normal, _diffuse, _specular, _roughness)

        # Bind our output shader to the surface material
        _material.CreateSurfaceOutput().ConnectToSource(_output.ConnectableAPI(), 'surface')
        _material.CreateDisplacementOutput().ConnectToSource(_output.ConnectableAPI(), 'displacement')
        return _material


def import_glb(_input_path):
        bpy.ops.wm.read_factory_settings(use_empty=True)
        bpy.ops.import_scene.gltf(
                filepath=_input_path,
        )
        pass


def export_usda(_output_path):
        bpy.ops.wm.usd_export(
                filepath=_output_path,
                selected_objects_only=True,
                export_animation=False,
                export_materials=False,
                convert_orientation=True,
                merge_parent_xform=True,
        )
        pass


def export_usdz(_base_name, _texture_format, _normal_path, _diffuse_path, _specular_path, _roughness_path):
        print("Compressing usdz archive...")
        # Create a writer for the target usdz file
        writer = Sdf.ZipFileWriter.CreateNew(f'{_base_name}.usdz')

        # Add files to the archive
        writer.AddFile(f'{_base_name}.usda', 'model.usda')
        writer.AddFile(_normal_path, f'textures/normal.{_texture_format}')
        writer.AddFile(_diffuse_path, f'textures/diffuse.{_texture_format}')
        writer.AddFile(_specular_path, f'textures/specular.{_texture_format}')
        writer.AddFile(_roughness_path, f'textures/roughness.{_texture_format}')

        # Finalize the file
        writer.Save()
        pass

def cleanup(_base_name):
        print('Removing temporary files...')
        os.remove(f'{_base_name}.usda')
        pass


if __name__ == '__main__':
        if len(sys.argv) < 7:
                print('Invalid number of arguments.')
                exit(1)
        glb_file = sys.argv[1]
        texture_format = sys.argv[2]
        normal_texture = sys.argv[3]
        diffuse_texture = sys.argv[4]
        specular_texture = sys.argv[5]
        roughness_texture = sys.argv[6]

        # Get the base name of the glb file
        base_name = glb_file.rsplit('.', 1)[0]

        # Convert the glb data to usd data
        print('Converting glb to usda...')
        import_glb(glb_file)
        export_usda(f'{base_name}.usda')

        # Open the new usd on the stage
        stage = Usd.Stage.Open(f'{base_name}.usda')

        # Get our mesh
        mesh = find_mesh(stage)
        mesh_path = mesh.GetPath()

        # Generate our new material
        material = gen_material(stage, mesh_path, texture_format)

        # Bind the material to the mesh
        UsdShade.MaterialBindingAPI(mesh).Bind(material)

        # Write our material graph to the usda file
        stage.Export(f'{base_name}.usda')

        # Pack and export our model
        export_usdz(base_name, texture_format, normal_texture, diffuse_texture, specular_texture, roughness_texture)

        # Clean up no longer needed files
        cleanup(base_name)
        print('Finished!')
        exit(0)