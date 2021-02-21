xof 0303txt 0032
template XSkinMeshHeader {
 <3cf169ce-ff7c-44ab-93c0-f78f62d172e2>
 WORD nMaxSkinWeightsPerVertex;
 WORD nMaxSkinWeightsPerFace;
 WORD nBones;
}

template VertexDuplicationIndices {
 <b8d65549-d7c9-4995-89cf-53a9a8b031e3>
 DWORD nIndices;
 DWORD nOriginalVertices;
 array DWORD indices[nIndices];
}

template SkinWeights {
 <6f0d123b-bad2-4167-a0d0-80224f25fabb>
 STRING transformNodeName;
 DWORD nWeights;
 array DWORD vertexIndices[nWeights];
 array FLOAT weights[nWeights];
 Matrix4x4 matrixOffset;
}

template AnimTicksPerSecond {
 <9e415a43-7ba6-4a73-8743-b73d47e88476>
 DWORD AnimTicksPerSecond;
}


AnimTicksPerSecond {
 24;
}

Mesh  {
 24;
 -128.000000;0.000000;88.000000;,
 -128.000000;4.000000;-88.000000;,
 -128.000000;4.000000;88.000000;,
 -128.000000;0.000000;-88.000000;,
 128.000000;4.000000;-88.000000;,
 128.000000;0.000000;-88.000000;,
 128.000000;4.000000;88.000000;,
 128.000000;0.000000;88.000000;,
 -128.000000;0.000000;88.000000;,
 -128.000000;0.000000;88.000000;,
 -128.000000;4.000000;-88.000000;,
 -128.000000;4.000000;-88.000000;,
 -128.000000;4.000000;88.000000;,
 -128.000000;4.000000;88.000000;,
 -128.000000;0.000000;-88.000000;,
 -128.000000;0.000000;-88.000000;,
 128.000000;4.000000;-88.000000;,
 128.000000;4.000000;-88.000000;,
 128.000000;0.000000;-88.000000;,
 128.000000;0.000000;-88.000000;,
 128.000000;4.000000;88.000000;,
 128.000000;4.000000;88.000000;,
 128.000000;0.000000;88.000000;,
 128.000000;0.000000;88.000000;;
 12;
 3;9,13,11;,
 3;9,11,15;,
 3;14,10,17;,
 3;14,17,19;,
 3;18,16,21;,
 3;18,21,23;,
 3;22,20,12;,
 3;22,12,8;,
 3;3,5,7;,
 3;3,7,0;,
 3;4,1,2;,
 3;4,2,6;;

 MeshNormals {
  24;
  0.000000;-1.000000;0.000000;,
  0.000000;1.000000;0.000000;,
  0.000000;1.000000;0.000000;,
  0.000000;-1.000000;0.000000;,
  0.000000;1.000000;0.000000;,
  0.000000;-1.000000;0.000000;,
  0.000000;1.000000;0.000000;,
  0.000000;-1.000000;0.000000;,
  0.000000;0.000000;1.000000;,
  -1.000000;0.000000;0.000000;,
  0.000000;0.000000;-1.000000;,
  -1.000000;0.000000;0.000000;,
  0.000000;0.000000;1.000000;,
  -1.000000;0.000000;0.000000;,
  0.000000;0.000000;-1.000000;,
  -1.000000;0.000000;0.000000;,
  1.000000;0.000000;0.000000;,
  0.000000;0.000000;-1.000000;,
  1.000000;0.000000;0.000000;,
  0.000000;0.000000;-1.000000;,
  0.000000;0.000000;1.000000;,
  1.000000;0.000000;0.000000;,
  0.000000;0.000000;1.000000;,
  1.000000;0.000000;0.000000;;
  12;
  3;9,13,11;,
  3;9,11,15;,
  3;14,10,17;,
  3;14,17,19;,
  3;18,16,21;,
  3;18,21,23;,
  3;22,20,12;,
  3;22,12,8;,
  3;3,5,7;,
  3;3,7,0;,
  3;4,1,2;,
  3;4,2,6;;
 }

 MeshTextureCoords {
  24;
  0.600120;-0.000009;,
  1.000002;-1.000007;,
  1.000002;0.000005;,
  0.600146;-0.999991;,
  0.000001;-0.999998;,
  0.564958;-0.999995;,
  0.000003;0.000007;,
  0.564931;-0.000013;,
  0.375000;-1.000000;,
  0.543143;-0.000005;,
  0.625000;-0.058548;,
  0.583098;-0.999996;,
  0.625000;-1.000000;,
  0.583098;-0.000005;,
  0.375000;-0.058548;,
  0.543143;-0.999996;,
  0.557461;-0.999945;,
  0.625000;-0.110634;,
  0.585904;-1.000020;,
  0.375000;-0.110634;,
  0.625000;-0.966035;,
  0.557461;0.000001;,
  0.375000;-0.966035;,
  0.585905;-0.000073;;
 }

 VertexDuplicationIndices {
  24;
  8;
  0,
  1,
  2,
  3,
  4,
  5,
  6,
  7,
  0,
  0,
  1,
  1,
  2,
  2,
  3,
  3,
  4,
  4,
  5,
  5,
  6,
  6,
  7,
  7;
 }

 MeshMaterialList {
  1;
  12;
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0;

  Material material_1 {
   1.000000;1.000000;1.000000;1.000000;;
   11.313703;
   1.000000;1.000000;1.000000;;
   0.000000;0.000000;0.000000;;

   TextureFilename {
    "janitor.png";
   }
  }
 }

 XSkinMeshHeader {
  0;
  0;
  0;
 }
}