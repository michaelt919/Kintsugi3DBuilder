import argparse
import sys
import bpy


def parse_args():
        if '--' in sys.argv:
                script_args = sys.argv[sys.argv.index('--') + 1:]
        else:
                script_args = []

        parser = argparse.ArgumentParser(description='Process custom Blender arguments.')
        parser.add_argument('--model', type=str, default='model.glb', help='Model location')
        parser.add_argument('--normal', type=str, default='normal.png', help='Normal texture location')
        parser.add_argument('--diffuse', type=str, default='diffuse.png', help='Diffuse texture location')
        parser.add_argument('--specular', type=str, default='specular.png', help='Specular texture location')
        parser.add_argument('--roughness', type=str, default='roughness.png', help='Roughness texture location')
        parser.add_argument('--use-cycles', action='store_true', help='Use Cycles rendering')

        return parser.parse_known_args(script_args)[0]


def main():
        args = parse_args()

        # Open the model in Blender
        bpy.ops.wm.read_factory_settings(use_empty=True)
        bpy.ops.import_scene.gltf(
                filepath=args.model,
        )

        # Grab the material off the model or create a new one
        model = bpy.context.view_layer.objects.active
        material = model.material_slots[0].material or bpy.data.materials.new("Specular Material")
        material.use_nodes = True

        # Acquire the node tree and clean
        nodes = material.node_tree.nodes
        links = material.node_tree.links
        for node in nodes:
                nodes.remove(node)

        # Generate and load nodes
        node_output = nodes.new('ShaderNodeOutputMaterial')
        node_output.location = (300, 0)

        if args.use_cycles:
                bpy.context.scene.render.engine = 'CYCLES'
                bpy.context.scene.cycles.shading_system = True
                script_text = bpy.data.texts.get('k3d_cycles.osl') or bpy.data.texts.new('k3d_cycles.osl')
                script_text.from_string(
                        'shader kintsugi3d('
                        '       color Diffuse = color(0.0, 0.0, 0.0),'
                        '       color Specular = color(0.0, 0.0, 0.0),'
                        '       float Roughness = 1.0,'
                        '       normal Normal = N,'
                        '       output closure color BSDF = oren_nayar_diffuse_bsdf(N, color(0.0, 0.0, 0.0), 0.0)'
                        ') {'
                        '       float RoughnessSq = Roughness * Roughness;'
                        '       BSDF = oren_nayar_diffuse_bsdf(Normal, Diffuse, 0.0)'
                        '               + generalized_schlick_bsdf(Normal, normalize(dPdu), color(1.0, 1.0, 1.0), color(0.0, 0.0, 0.0), RoughnessSq, RoughnessSq, Specular, color(1.0, 1.0, 1.0), 5.0, "ggx");'
                        '}'
                )
                node_shader = nodes.new('ShaderNodeScript')
                node_shader.mode = 'INTERNAL'
                node_shader.script = script_text
                node_shader.update()
        else:
                node_shader = nodes.new("ShaderNodeEeveeSpecular")
        node_shader.location = (100, 0)

        texture_normal = nodes.new('ShaderNodeTexImage')
        texture_normal.image = bpy.data.images.load(args.normal)
        texture_normal.image.colorspace_settings.name = 'Non-Color'
        texture_normal.location = (-400, -400)
        node_normal = nodes.new('ShaderNodeNormalMap')
        node_normal.location = (-100, -150)

        texture_diffuse = nodes.new('ShaderNodeTexImage')
        texture_diffuse.image = bpy.data.images.load(args.diffuse)
        texture_diffuse.location = (-300, 350)

        texture_specular = nodes.new('ShaderNodeTexImage')
        texture_specular.image = bpy.data.images.load(args.specular)
        texture_specular.location = (-600, 150)

        texture_roughness = nodes.new('ShaderNodeTexImage')
        texture_roughness.image = bpy.data.images.load(args.roughness)
        texture_roughness.image.colorspace_settings.name = 'Non-Color'
        texture_roughness.location = (-600, -150)

        # Generate links
        links.new(texture_normal.outputs['Color'], node_normal.inputs['Color'])
        links.new(node_normal.outputs['Normal'], node_shader.inputs['Normal'])
        if args.use_cycles:
                links.new(texture_diffuse.outputs['Color'], node_shader.inputs['Diffuse'])
        else:
                links.new(texture_diffuse.outputs['Color'], node_shader.inputs['Base Color'])
        links.new(texture_specular.outputs['Color'], node_shader.inputs['Specular'])
        links.new(texture_roughness.outputs['Color'], node_shader.inputs['Roughness'])
        links.new(node_shader.outputs['BSDF'], node_output.inputs['Surface'])

        # Apply the material
        if model is not None:
                model.data.materials.clear()
                model.data.materials.append(material)


if __name__ == '__main__':
        main()
