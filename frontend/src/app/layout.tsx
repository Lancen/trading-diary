import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Trading Diary",
  description: "交易日记应用",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-background antialiased">{children}</body>
    </html>
  );
}
