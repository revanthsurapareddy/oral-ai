import { Inter } from "next/font/google";
import "./globals.css";
import { DataStoreProvider } from "@/context/DataStoreContext";

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata = {
  title: "OralAI Scan - Oral Cancer Margin Detection",
  description: "Real-time AI Oral Cancer Margin Detection and Segmentation",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <DataStoreProvider>
          <div className="app-container">
            {children}
          </div>
        </DataStoreProvider>
      </body>
    </html>
  );
}
