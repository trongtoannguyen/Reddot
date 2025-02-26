//
//  ContentView.swift
//  ReddotMobile
//
//  Created by Nguyen Trong Toan on 17/1/25.
//

import SwiftUI

struct ContentView: View {
    let attributedString = try! AttributedString(
        markdown: "_Hamlet_ by William Shakespeare")

    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text(attributedString)
                .font(.system(size: 18, weight: .light,design: .serif))
                .padding()
                .background(Color.yellow, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            Button(/*@START_MENU_TOKEN@*/"Button"/*@END_MENU_TOKEN@*/) {
            }
            
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
