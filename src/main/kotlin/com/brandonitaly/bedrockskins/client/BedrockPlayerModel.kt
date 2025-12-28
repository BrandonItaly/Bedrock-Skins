package com.brandonitaly.bedrockskins.client

import com.brandonitaly.bedrockskins.bedrock.BedrockBone
import com.brandonitaly.bedrockskins.bedrock.BedrockGeometry
import net.minecraft.client.model.*
import net.minecraft.client.render.entity.model.EntityModelPartNames
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
 
class BedrockPlayerModel(
    val root: ModelPart, 
    thinArms: Boolean, 
    @JvmField val partsMap: Map<String, ModelPart>,
    @JvmField val defaultTransforms: Map<String, PartTransform>,
    val animationArmsOutFront: Boolean,
    val animationStationaryLegs: Boolean
) : PlayerEntityModel(root, thinArms) {

    data class PartTransform(val x: Float, val y: Float, val z: Float, val pitch: Float, val yaw: Float, val roll: Float)

    // Vertical offsets to apply to armor / cape feature rendering so they line up
    // with Bedrock geometry when that geometry uses non-standard pivots.
    @JvmField
    var armorYOffset: Float = 0f
    @JvmField
    var capeYOffset: Float = 0f
    @JvmField
    var upperArmorYOffset: Float = 0f

    companion object {
        fun create(geometry: BedrockGeometry, thinArms: Boolean): BedrockPlayerModel {
            // Ensure geometry has all required vanilla bones before building
            validateAndPatchGeometry(geometry)

            val (root, parts, defaults) = buildRoot(geometry)
            return BedrockPlayerModel(
                root,
                thinArms,
                parts,
                defaults,
                geometry.animationArmsOutFront ?: false,
                geometry.animationStationaryLegs ?: false
            )
        }

        /**
         * Checks the geometry for missing standard bones (Head, Body, Arms, etc.)
         * and injects dummy bones if they are missing. This prevents the PlayerEntityModel
         * constructor from crashing when looking for required children.
         */
        private fun validateAndPatchGeometry(geometry: BedrockGeometry) {
            val requiredBones = mapOf(
                "head" to "body",
                "hat" to "head",
                "body" to null,
                "jacket" to "body",
                "leftArm" to "body",
                "leftSleeve" to "leftArm",
                "rightArm" to "body",
                "rightSleeve" to "rightArm",
                "leftLeg" to "body",
                "leftPants" to "leftLeg",
                "rightLeg" to "body",
                "rightPants" to "rightLeg"
            )

            // Safely get mutable list of current bones
            val currentBones = geometry.bones?.toMutableList() ?: mutableListOf()
            
            // normalize existing names to lowercase for comparison
            val existingBoneNames = currentBones.mapNotNull { it.name?.lowercase() }.toSet()
            val newBones = mutableListOf<BedrockBone>()

            for ((boneName, parentName) in requiredBones) {
                // Check if the bone exists
                val isMissing = !existingBoneNames.contains(boneName.lowercase()) && 
                                !existingBoneNames.contains(mapBoneName(boneName).lowercase())

                if (isMissing) {
                    println("BedrockPlayerModel: Patching geometry - adding missing bone: $boneName")
                    newBones.add(
                        BedrockBone(
                            name = boneName,
                            parent = parentName,
                            pivot = listOf(0f, 0f, 0f),
                            rotation = listOf(0f, 0f, 0f),
                            cubes = emptyList(),
                            mirror = false,
                            locators = null,
                            inflate = null
                        )
                    )
                }
            }

            // Update the geometry with the new bones
            if (newBones.isNotEmpty()) {
                geometry.bones = currentBones + newBones
            }
        }

        private fun buildRoot(geometry: BedrockGeometry): Triple<ModelPart, Map<String, ModelPart>, Map<String, PartTransform>> {
            val modelData = ModelData()
            val rootData = modelData.root

            val boneMap = geometry.bones?.associateBy { it.name } ?: emptyMap()
            val processedBones = mutableSetOf<String>()

            fun addBone(boneName: String) {
                if (!processedBones.add(boneName)) return
                val bone = boneMap[boneName] ?: return
                bone.parent?.let { addBone(it) }
            }
            geometry.bones?.forEach { addBone(it.name) }

            val partDataMap = mutableMapOf<String, ModelPartData>()
            val vanillaRootParts = setOf(
                EntityModelPartNames.HEAD,
                EntityModelPartNames.BODY,
                EntityModelPartNames.RIGHT_ARM,
                EntityModelPartNames.LEFT_ARM,
                EntityModelPartNames.RIGHT_LEG,
                EntityModelPartNames.LEFT_LEG
            )
            val bonesToProcess = geometry.bones?.toMutableList() ?: mutableListOf()
            val defaultTransforms = mutableMapOf<String, PartTransform>()
            var stuckCounter = 0

            fun extractUV(cube: Any?): Pair<Int, Int> {
                return when (cube) {
                    is List<*> -> if (cube.size >= 2) ((cube[0] as Number).toInt() to (cube[1] as Number).toInt()) else 0 to 0
                    is Map<*, *> -> {
                        val uvList = cube["uv"] as? List<*>
                        if (uvList != null && uvList.size >= 2) ((uvList[0] as Number).toInt() to (uvList[1] as Number).toInt()) else 0 to 0
                    }
                    else -> 0 to 0
                }
            }

            while (bonesToProcess.isNotEmpty()) {
                val iterator = bonesToProcess.iterator()
                var processedAny = false
                while (iterator.hasNext()) {
                    val bone = iterator.next()
                    if (bone.parent == null || partDataMap.containsKey(bone.parent)) {
                        val parentData = if (bone.parent == null) rootData else partDataMap[bone.parent]!!
                        val builder = ModelPartBuilder.create()

                        bone.cubes?.forEach { cube ->
                            val (u, v) = extractUV(cube.uv)
                            val dilation = Dilation(cube.inflate ?: 0f)
                            val isMirrored = cube.mirror ?: bone.mirror ?: false
                            val bPx = bone.pivot?.get(0) ?: 0f
                            val bPy = bone.pivot?.get(1) ?: 0f
                            val bPz = bone.pivot?.get(2) ?: 0f
                            val cOx = cube.origin[0]
                            val cOy = cube.origin[1]
                            val cOz = cube.origin[2]
                            val offX = cOx - bPx
                            val offY = bPy - cOy - cube.size[1]
                            val offZ = cOz - bPz
                            builder.mirrored(isMirrored).uv(u, v).cuboid(
                                offX, offY, offZ,
                                cube.size[0], cube.size[1], cube.size[2],
                                dilation
                            )
                        }

                        val bPx = bone.pivot?.get(0) ?: 0f
                        val bPy = bone.pivot?.get(1) ?: 0f
                        val bPz = bone.pivot?.get(2) ?: 0f
                        var pX = bPx
                        var pY = 24f - bPy
                        var pZ = bPz
                        val vanillaName = mapBoneName(bone.name)
                        val parentForCreation = if (bone.parent != null && vanillaRootParts.contains(vanillaName)) rootData else parentData
                        if (bone.parent != null && parentForCreation !== rootData) {
                            val parentBone = boneMap[bone.parent]!!
                            val ppX = parentBone.pivot?.get(0) ?: 0f
                            val ppY = 24f - (parentBone.pivot?.get(1) ?: 0f)
                            val ppZ = parentBone.pivot?.get(2) ?: 0f
                            pX -= ppX
                            pY -= ppY
                            pZ -= ppZ
                        }
                        val rotX = Math.toRadians(-(bone.rotation?.get(0) ?: 0f).toDouble()).toFloat()
                        val rotY = Math.toRadians(-(bone.rotation?.get(1) ?: 0f).toDouble()).toFloat()
                        val rotZ = Math.toRadians((bone.rotation?.get(2) ?: 0f).toDouble()).toFloat()
                        val transform = ModelTransform.of(pX, pY, pZ, rotX, rotY, rotZ)
                        val partData = parentForCreation.addChild(vanillaName, builder, transform)
                        partDataMap[bone.name] = partData
                        defaultTransforms[bone.name] = PartTransform(pX, pY, pZ, rotX, rotY, rotZ)
                        if (vanillaName != bone.name) {
                            defaultTransforms[vanillaName] = PartTransform(pX, pY, pZ, rotX, rotY, rotZ)
                        }
                        iterator.remove()
                        processedAny = true
                    }
                }
                if (!processedAny) {
                    stuckCounter++
                    if (stuckCounter > 5) break
                }
            }

            val texturedModelData = TexturedModelData.of(modelData, geometry.description.textureWidth, geometry.description.textureHeight)
            val rootPart = texturedModelData.createModel()

            val finalParts = mutableMapOf<String, ModelPart>()
            fun findPart(parent: ModelPart, boneName: String): ModelPart? {
                val bone = boneMap[boneName] ?: return null
                val mappedRootName = mapBoneName(bone.name)
                if (parent.hasChild(mappedRootName)) {
                    return parent.getChild(mappedRootName)
                }
                val path = generateSequence(bone) { b -> b.parent?.let { boneMap[it] } }
                    .map { mapBoneName(it.name) }
                    .toList().asReversed()
                var currPart = parent
                for (segment in path) {
                    if (currPart.hasChild(segment)) {
                        currPart = currPart.getChild(segment)
                    } else {
                        return null
                    }
                }
                return currPart
            }
            boneMap.keys.forEach { name ->
                findPart(rootPart, name)?.let { finalParts[name] = it }
            }
            return Triple(rootPart, finalParts, defaultTransforms)
        }

        fun mapBoneName(name: String): String {
            val lower = name.lowercase()
            return when {
                lower == "head" -> EntityModelPartNames.HEAD // "head"
                lower == "hat" || lower == "headwear" -> EntityModelPartNames.HAT // "hat"
                lower == "body" -> EntityModelPartNames.BODY // "body"
                lower == "jacket" -> EntityModelPartNames.JACKET // "jacket"
                
                // Arms
                lower == "rightarm" || lower == "right_arm" -> EntityModelPartNames.RIGHT_ARM // "right_arm"
                lower == "leftarm" || lower == "left_arm" -> EntityModelPartNames.LEFT_ARM // "left_arm"
                
                // Legs
                lower == "rightleg" || lower == "right_leg" -> EntityModelPartNames.RIGHT_LEG // "right_leg"
                lower == "leftleg" || lower == "left_leg" -> EntityModelPartNames.LEFT_LEG // "left_leg"
                
                // Sleeves/Pants
                lower == "rightsleeve" || lower == "right_sleeve" -> "right_sleeve"
                lower == "leftsleeve" || lower == "left_sleeve" -> "left_sleeve"
                lower == "rightpants" || lower == "right_pants" -> "right_pants"
                lower == "leftpants" || lower == "left_pants" -> "left_pants"
                
                else -> name 
            }
        }
    }
    
   override fun setAngles(state: PlayerEntityRenderState) {
        super.setAngles(state)
        
        if (animationArmsOutFront) {
            setArmAngle(partsMap["rightArm"] ?: partsMap["right_arm"])
            setArmAngle(partsMap["leftArm"] ?: partsMap["left_arm"])
        }

        if (animationStationaryLegs) {
            resetLegAngle("rightLeg", "right_leg")
            resetLegAngle("leftLeg", "left_leg")
        }
    }

    private fun setArmAngle(part: ModelPart?) {
        part?.apply {
            pitch = -1.5707964f
            yaw = 0f
            roll = 0f
        }
    }

    private fun resetLegAngle(key1: String, key2: String) {
        val leg = partsMap[key1] ?: partsMap[key2] ?: return
        val def = defaultTransforms[key1] ?: defaultTransforms[key2] ?: return
        leg.pitch = def.pitch
        leg.yaw = def.yaw
        leg.roll = def.roll
    }
    
    fun copyFromVanilla(vanillaModel: PlayerEntityModel) {
        fun copyRot(bedrockName: String, vanillaPart: ModelPart) {
            val part = partsMap[bedrockName] ?: partsMap[mapBoneName(bedrockName)]
            part?.let {
                it.pitch = vanillaPart.pitch
                it.yaw = vanillaPart.yaw
                it.roll = vanillaPart.roll
            }
        }

        // Copy main parts
        listOf(
            "head" to vanillaModel.head,
            "body" to vanillaModel.body,
            "hat" to vanillaModel.hat
        ).forEach { (name, part) -> copyRot(name, part) }

        if (!animationArmsOutFront) {
            copyRot("rightArm", vanillaModel.rightArm)
            copyRot("leftArm", vanillaModel.leftArm)
        }
        if (!animationStationaryLegs) {
            copyRot("rightLeg", vanillaModel.rightLeg)
            copyRot("leftLeg", vanillaModel.leftLeg)
        }

        // Helper to get pivotY reflectively
        fun getPivotY(part: ModelPart): Float {
            return try {
                val field = part.javaClass.getDeclaredField("pivotY")
                field.isAccessible = true
                (field.get(part) as? Number)?.toFloat() ?: 0f
            } catch (_: Exception) {
                try {
                    val field = net.minecraft.client.model.ModelPart::class.java.getDeclaredField("pivotY")
                    field.isAccessible = true
                    field.getFloat(part)
                } catch (_: Exception) {
                    0f
                }
            }
        }

        // Compute approximate vertical offsets for armor/cape so feature renderers can be translated to match Bedrock geometry
        try {
            val bodyTransform = defaultTransforms["body"] ?: defaultTransforms["BODY"]
            val bedrockBodyY = bodyTransform?.y ?: 0f
            val vanillaBodyPivotY = getPivotY(vanillaModel.body)

            val bedrockHeadY = defaultTransforms["head"]?.y ?: bedrockBodyY
            val vanillaHeadPivotY = getPivotY(vanillaModel.head)

            // Upper armor (helmet + chest) — average head and body deltas
            upperArmorYOffset = ((bedrockBodyY + bedrockHeadY) * 0.5f) - ((vanillaBodyPivotY + vanillaHeadPivotY) * 0.5f)
            armorYOffset = upperArmorYOffset

            // Compute cape and elytra offsets from specific bone pivots if present
            val bedrockCapeY = defaultTransforms["cape"]?.y ?: bedrockBodyY
            capeYOffset = bedrockCapeY - vanillaBodyPivotY

            val bedrockElytraY = defaultTransforms["elytra"]?.y ?: bedrockCapeY
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}