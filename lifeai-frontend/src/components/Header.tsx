interface HeaderProps {
  title: string
  subtitle?: string
}

export const Header = ({ title, subtitle }: HeaderProps) => {
  return (
    <header className="bg-white shadow-sm py-6 px-4 mb-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900">{title}</h1>
        {subtitle && (
          <p className="mt-2 text-gray-600">{subtitle}</p>
        )}
      </div>
    </header>
  )
}
