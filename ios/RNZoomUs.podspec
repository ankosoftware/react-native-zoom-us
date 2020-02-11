require "json"

package = JSON.parse(File.read(File.join(__dir__, "../package.json")))

Pod::Spec.new do |s|
  s.name         = "RNZoomUs"
  s.version      = "1.0.0"
  s.summary      = "RNZoomUs"
  s.description  = <<-DESC
                  RNZoomUs
                   DESC
  s.homepage     = package["homepage"]
  s.license      = "MIT"
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "8.0"
  s.source       = { :git => "https://github.com/mieszko4/react-native-zoom-us.git", :tag => "master" }
  s.source_files  = "**/*.{h,m}"
  s.requires_arc = true
  s.dependency "React"
end

  