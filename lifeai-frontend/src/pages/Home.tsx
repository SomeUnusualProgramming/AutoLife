import { Header } from '../components'

export const Home = () => {
  return (
    <div>
      <Header 
        title="Welcome to LifeAI"
        subtitle="Intelligent Life Management System"
      />
      <main className="max-w-7xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Feature 1</h2>
            <p className="text-gray-600">Coming soon...</p>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Feature 2</h2>
            <p className="text-gray-600">Coming soon...</p>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Feature 3</h2>
            <p className="text-gray-600">Coming soon...</p>
          </div>
        </div>
      </main>
    </div>
  )
}
