# Findings: 018 Location-Aware POI Display

## Review Result
- Score: 9/10 (PASS)
- Date: 2026-04-06

## Key Discoveries

1. **Geospatial + ArDevice Heading統合**: Geospatial APIのlat/lngとArDeviceのQuaternion回転から
   デバイスのheading(yaw)を算出し、POIとの相対方向を8方位で表示できる。

2. **Haversine距離計算**: 緯度経度からメートル単位の距離を正確に計算。
   1km未満は"500m"、1km以上は"1.5km"形式でフォーマット。

3. **相対方向の8方位分類**: POIのbearing角とデバイスのheading角の差分を
   22.5度刻みで8方位(Ahead/Front Right/Right/Behind Right/Behind/Behind Left/Left/Front Left)に分類。

4. **Top-3最近POIフィルタリング**: 全POIを距離でソートし上位3件のみ表示することで、
   FOV制約に適合しつつ最も関連性の高い情報を提供。

5. **Quaternion→Heading変換**: Pose.rotationのQuaternion(x,y,z,w)からyaw角を
   2*(w*z+x*y)と1-2*(y*y+z*z)のatan2で算出する手法を確立。

## Extracted Patterns
- patterns/ar-patterns.md: Geospatial + Heading POI pattern
- patterns/architecture-patterns.md: Robust lifecycle pattern (reused)
