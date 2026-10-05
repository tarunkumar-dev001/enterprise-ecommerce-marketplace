import { Component, useRef } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Float, PresentationControls } from '@react-three/drei'
import { Canvas, useFrame } from '@react-three/fiber'
import type { Group } from 'three'

class SceneBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  componentDidCatch(error: Error, info: ErrorInfo) { void error; void info }
  render() {
    if (this.state.failed) return <div aria-hidden="true" className="grid h-72 place-items-center sm:h-96 lg:h-[30rem]"><div className="relative grid h-56 w-56 place-items-center rounded-full border border-indigo-100 bg-gradient-to-br from-white to-indigo-50 shadow-xl shadow-indigo-950/10"><div className="grid h-28 w-24 place-items-center rounded-2xl bg-indigo-700 text-4xl text-white shadow-lg">M</div><span className="absolute right-5 top-8 h-8 w-8 rounded-xl bg-amber-300 shadow-md"/><span className="absolute bottom-8 left-4 h-7 w-7 rounded-full bg-rose-300 shadow-md"/></div></div>
    return this.props.children
  }
}

function MarketplaceObjects() {
  const display = useRef<Group>(null)

  useFrame(({ clock }, delta) => {
    if (!display.current) return
    display.current.rotation.y += delta * 0.16
    display.current.position.y = Math.sin(clock.elapsedTime * 0.8) * 0.08
  })

  return (
    <group ref={display}>
      <mesh position={[0, -1.12, 0]} receiveShadow>
        <cylinderGeometry args={[1.65, 1.8, 0.22, 64]} />
        <meshStandardMaterial color="#e7e5e4" roughness={0.72} />
      </mesh>

      <Float speed={1.5} rotationIntensity={0.12} floatIntensity={0.18}>
        <group position={[-0.48, -0.1, 0.1]} rotation={[0.04, -0.28, -0.04]}>
          <mesh castShadow position={[0, 0.3, 0]}>
            <boxGeometry args={[0.95, 1.35, 0.78]} />
            <meshStandardMaterial color="#065f46" roughness={0.38} />
          </mesh>
          <mesh castShadow position={[0, 0.98, 0]} rotation={[Math.PI / 2, 0, 0]}>
            <torusGeometry args={[0.27, 0.045, 12, 40, Math.PI]} />
            <meshStandardMaterial color="#d6a85f" metalness={0.38} roughness={0.32} />
          </mesh>
          <mesh position={[0, 0.31, 0.405]}>
            <boxGeometry args={[0.42, 0.04, 0.018]} />
            <meshStandardMaterial color="#d6a85f" metalness={0.3} roughness={0.4} />
          </mesh>
        </group>
      </Float>

      <Float speed={1.9} rotationIntensity={0.2} floatIntensity={0.25}>
        <group position={[0.64, -0.22, 0.12]} rotation={[0.08, 0.38, 0.08]}>
          <mesh castShadow>
            <boxGeometry args={[0.9, 0.9, 0.9]} />
            <meshStandardMaterial color="#d6a85f" roughness={0.42} />
          </mesh>
          <mesh position={[0, 0, 0.46]}>
            <boxGeometry args={[0.12, 0.92, 0.025]} />
            <meshStandardMaterial color="#f5f5f4" roughness={0.6} />
          </mesh>
          <mesh position={[0, 0, 0.475]}>
            <boxGeometry args={[0.92, 0.12, 0.025]} />
            <meshStandardMaterial color="#f5f5f4" roughness={0.6} />
          </mesh>
        </group>
      </Float>

      <Float speed={1.2} rotationIntensity={0.3} floatIntensity={0.35}>
        <mesh castShadow position={[0.05, 0.92, 0.2]}>
          <sphereGeometry args={[0.22, 32, 24]} />
          <meshStandardMaterial color="#b4533c" roughness={0.3} />
        </mesh>
      </Float>

      <mesh position={[-1.35, 0.85, -0.55]}>
        <sphereGeometry args={[0.07, 20, 16]} />
        <meshStandardMaterial color="#d6a85f" metalness={0.45} roughness={0.35} />
      </mesh>
      <mesh position={[1.35, 0.7, -0.4]}>
        <sphereGeometry args={[0.11, 20, 16]} />
        <meshStandardMaterial color="#065f46" roughness={0.4} />
      </mesh>
    </group>
  )
}

export default function DeveloperScene() {
  return (
    <div
      aria-hidden="true"
      className="relative isolate h-72 min-w-0 w-full touch-pan-y overflow-hidden sm:h-96 lg:h-[30rem]"
    >
      <SceneBoundary><Canvas
        camera={{ position: [0, 0.35, 5.2], fov: 38 }}
        dpr={[1, 1.5]}
        gl={{ antialias: true, alpha: true }}
        style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', touchAction: 'pan-y' }}
        onCreated={({ gl }) => gl.setClearColor('#000000', 0)}
      >
        <ambientLight intensity={1.35} />
        <directionalLight castShadow intensity={3} position={[3, 5, 4]} />
        <directionalLight intensity={1.1} position={[-4, 1, -2]} color="#d6a85f" />
        <PresentationControls
          global={false}
          cursor={false}
          snap={0.2}
          rotation={[0, -0.15, 0]}
          polar={[-0.15, 0.2]}
          azimuth={[-0.45, 0.45]}
        >
          <MarketplaceObjects />
        </PresentationControls>
      </Canvas></SceneBoundary>
    </div>
  )
}


