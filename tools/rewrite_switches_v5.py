#!/usr/bin/env python3
"""
Precision script to rewrite pattern matching switch expressions to if-else chains.
Fixed context detection for casts and complex expressions.
"""

import re
from pathlib import Path

class SwitchRewriter:
    def __init__(self, content):
        self.content = content
        self.chars = list(content)
        self.length = len(content)
        self.pos = 0
        self.output = []
    
    def peek(self, offset=0):
        if self.pos + offset < self.length:
            return self.chars[self.pos + offset]
        return '\0'
    
    def consume(self):
        if self.pos < self.length:
            ch = self.chars[self.pos]
            self.pos += 1
            return ch
        return '\0'
    
    def match_keyword(self, keyword):
        """Check if keyword matches at current position (word boundary)."""
        if self.pos + len(keyword) <= self.length:
            if self.content[self.pos:self.pos+len(keyword)] == keyword:
                if self.pos + len(keyword) == self.length or not self.content[self.pos+len(keyword)].isalnum() and self.content[self.pos+len(keyword)] != '_':
                    if self.pos == 0 or not self.content[self.pos-1].isalnum() and self.content[self.pos-1] != '_':
                        return True
        return False
    
    def find_switch_context(self, switch_start):
        """Determine the context of a switch at switch_start position."""
        # Look backwards, skipping whitespace
        i = switch_start - 1
        while i >= 0 and self.content[i].isspace():
            i -= 1
        
        # Check if we're inside a cast expression like (U)(switch...)
        # Look for opening paren before switch
        if i >= 0 and self.content[i] == ')':
            # Find matching (
            paren = 1
            k = i - 1
            while k >= 0 and paren > 0:
                if self.content[k] == ')':
                    paren += 1
                elif self.content[k] == '(':
                    paren -= 1
                k -= 1
            # Check if it's a cast: (Type)
            cast_text = self.content[k+1:i+1]
            if re.match(r'^\(\s*\w+(?:\s*[\?\<\>\[\],]*)*\s*\)$', cast_text):
                # Continue looking before the cast
                return self.find_switch_context(k + 1)
        
        # Check for return keyword (must be a complete word)
        if i >= 5:
            if self.content[i-5:i+1] == 'return':
                # Check word boundary before 'r'
                if i == 5 or not (self.content[i-6].isalnum() or self.content[i-6] == '_'):
                    # Check if there's a cast between return and switch
                    # Look at text between return and switch
                    between = self.content[i+1:switch_start]
                    if re.search(r'\(\s*\w+\s*\)', between):
                        # There's a cast - treat as expression, not direct return
                        return 'cast_return', None
                    return 'return', None
        
        # Check for assignment: look for = not inside parens
        j = i
        paren_count = 0
        while j >= 0:
            ch = self.content[j]
            if ch == ')':
                paren_count += 1
            elif ch == '(':
                paren_count -= 1
            elif ch == '=' and paren_count == 0:
                # Found assignment, find target
                k = j - 1
                while k >= 0 and self.content[k].isspace():
                    k -= 1
                end = k + 1
                while k >= 0 and (self.content[k].isalnum() or self.content[k] in '._'):
                    k -= 1
                target = self.content[k+1:end]
                return 'assign', target
            elif ch == ';' and paren_count == 0:
                break
            elif ch == '{' and paren_count == 0:
                break
            j -= 1
        
        return 'statement', None
    
    def parse_switch_block(self):
        """Parse a switch block starting at current position (after 'switch')."""
        # We're at 'switch', consume it
        for _ in range(6):
            self.consume()
        
        # Skip whitespace
        while self.peek().isspace():
            self.consume()
        
        # Expect '('
        if self.peek() != '(':
            return None
        self.consume()  # '('
        
        # Extract subject
        subject = self.extract_until(')', allow_nested=True)
        if self.peek() == ')':
            self.consume()
        
        # Skip whitespace
        while self.peek().isspace():
            self.consume()
        
        # Expect '{'
        if self.peek() != '{':
            return None
        
        # Find matching brace
        brace_start = self.pos
        brace_count = 1
        self.consume()  # '{'
        
        cases = []
        while self.pos < self.length and brace_count > 0:
            ch = self.peek()
            
            if ch == '{':
                brace_count += 1
                self.consume()
            elif ch == '}':
                brace_count -= 1
                if brace_count == 0:
                    break
                self.consume()
            elif ch == 'c' and self.content[self.pos:self.pos+4] == 'case':
                # Parse case
                case_info = self.parse_case()
                if case_info:
                    cases.append(case_info)
            elif ch == 'd' and self.content[self.pos:self.pos+7] == 'default':
                # Parse default
                case_info = self.parse_default()
                if case_info:
                    cases.append(case_info)
            else:
                self.consume()
        
        brace_end = self.pos
        if self.peek() == '}':
            self.consume()  # consume the closing brace
        
        # Check for trailing semicolon (for switch expressions)
        while self.peek().isspace():
            self.consume()
        has_semicolon = False
        if self.peek() == ';':
            has_semicolon = True
            self.consume()
        
        return {
            'subject': subject.strip(),
            'cases': cases,
            'has_semicolon': has_semicolon,
            'brace_start': brace_start,
            'brace_end': brace_end
        }
    
    def extract_until(self, stop_char, allow_nested=True):
        """Extract text until stop_char (respecting nesting)."""
        start = self.pos
        paren = 0
        brace = 0
        bracket = 0
        in_string = False
        string_char = None
        escaped = False
        
        while self.pos < self.length:
            ch = self.chars[self.pos]
            
            if escaped:
                escaped = False
                self.pos += 1
                continue
            
            if ch == '\\' and in_string:
                escaped = True
                self.pos += 1
                continue
            
            if not in_string:
                if ch in '"\'':
                    in_string = True
                    string_char = ch
                elif ch in '([{':
                    if ch == '(':
                        paren += 1
                    elif ch == '[':
                        bracket += 1
                    elif ch == '{':
                        brace += 1
                elif ch in ')]}':
                    if ch == ')':
                        paren -= 1
                        if paren < 0 and stop_char == ')':
                            break
                    elif ch == ']':
                        bracket -= 1
                        if bracket < 0 and stop_char == ']':
                            break
                    elif ch == '}':
                        brace -= 1
                        if brace < 0 and stop_char == '}':
                            break
                elif ch == stop_char and paren == 0 and brace == 0 and bracket == 0:
                    break
            else:
                if ch == string_char:
                    in_string = False
                    string_char = None
            
            self.pos += 1
        
        return self.content[start:self.pos]
    
    def parse_case(self):
        """Parse a case clause."""
        # Consume 'case'
        for _ in range(4):
            self.consume()
        
        # Skip whitespace
        while self.peek().isspace():
            self.consume()
        
        # Parse pattern
        pattern_start = self.pos
        arrow_type = None
        while self.pos < self.length:
            if self.peek() == '-' and self.pos + 1 < self.length and self.chars[self.pos + 1] == '>':
                arrow_type = '->'
                break
            elif self.peek() == ':':
                arrow_type = ':'
                break
            self.consume()
        else:
            return None
        
        pattern_text = self.content[pattern_start:self.pos].strip()
        
        # Check for pattern matching: Type var
        pattern_match = re.match(r'^(\S+)\s+(\w+)$', pattern_text)
        if pattern_match:
            case_type = 'pattern'
            type_name = pattern_match.group(1)
            var_name = pattern_match.group(2)
        else:
            case_type = 'constant'
            type_name = None
            var_name = None
            const_value = pattern_text
        
        # Consume -> or :
        if arrow_type == '->':
            self.consume()  # '-'
            self.consume()  # '>'
        else:
            self.consume()  # ':'
        
        # Skip whitespace
        while self.peek().isspace():
            self.consume()
        
        # Parse body
        body = self.parse_case_body(arrow_type)
        
        return {
            'type': case_type,
            'type_name': type_name,
            'var_name': var_name,
            'const_value': const_value if case_type == 'constant' else None,
            'arrow_type': arrow_type,
            'body': body
        }
    
    def parse_default(self):
        """Parse a default clause."""
        # Consume 'default'
        for _ in range(7):
            self.consume()
        
        # Skip whitespace
        while self.peek().isspace():
            self.consume()
        
        # Expect : or ->
        arrow_type = ':'
        if self.peek() == '-' and self.pos + 1 < self.length and self.chars[self.pos + 1] == '>':
            arrow_type = '->'
            self.consume()
            self.consume()
        elif self.peek() == ':':
            self.consume()
        
        # Skip whitespace
        while self.peek().isspace():
            self.consume()
        
        # Parse body
        body = self.parse_case_body(arrow_type)
        
        return {
            'type': 'default',
            'arrow_type': arrow_type,
            'body': body
        }
    
    def parse_case_body(self, arrow_type):
        """Parse case body until next case, default, or closing brace."""
        start = self.pos
        paren = 0
        brace = 0
        bracket = 0
        in_string = False
        string_char = None
        escaped = False
        
        while self.pos < self.length:
            ch = self.peek()
            
            if escaped:
                escaped = False
                self.consume()
                continue
            
            if ch == '\\' and in_string:
                escaped = True
                self.consume()
                continue
            
            if not in_string:
                if ch in '"\'':
                    in_string = True
                    string_char = ch
                elif ch in '([{':
                    if ch == '(':
                        paren += 1
                    elif ch == '[':
                        bracket += 1
                    elif ch == '{':
                        brace += 1
                elif ch in ')]}':
                    if ch == ')':
                        paren -= 1
                    elif ch == ']':
                        bracket -= 1
                    elif ch == '}':
                        brace -= 1
                        if brace < 0:
                            # End of switch
                            break
                elif paren == 0 and brace == 0 and bracket == 0:
                    # Check for next case or default
                    if ch == 'c' and self.content[self.pos:self.pos+4] == 'case':
                        if self.pos == start or self.content[self.pos-1].isspace():
                            break
                    elif ch == 'd' and self.content[self.pos:self.pos+7] == 'default':
                        if self.pos == start or self.content[self.pos-1].isspace():
                            break
            else:
                if ch == string_char:
                    in_string = False
                    string_char = None
            
            self.consume()
        
        return self.content[start:self.pos].strip()
    
    def generate_if_else(self, switch_info, context_type, assign_target, indent):
        """Generate if-else chain from switch info."""
        lines = []
        first = True
        subject = switch_info['subject']
        
        for case in switch_info['cases']:
            if case['type'] == 'pattern':
                type_name = case['type_name']
                var_name = case['var_name']
                body_lines = self.clean_body(case['body'], case['arrow_type'])
                
                if first:
                    lines.append(f"{indent}if ({subject} instanceof {type_name}) {{")
                else:
                    lines.append(f"{indent}else if ({subject} instanceof {type_name}) {{")
                
                lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {subject};")
                
                for bl in body_lines:
                    if context_type == 'return':
                        lines.append(f"{indent}    return {bl};")
                    elif context_type == 'cast_return':
                        # Cast return - the cast is handled at the return level
                        lines.append(f"{indent}    return {bl};")
                    elif context_type == 'assign':
                        lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        lines.append(f"{indent}    {bl}")
                
                lines.append(f"{indent}}}")
                first = False
            
            elif case['type'] == 'constant':
                const_val = case['const_value']
                body_lines = self.clean_body(case['body'], case['arrow_type'])
                
                if first:
                    lines.append(f"{indent}if ({subject} == {const_val}) {{")
                else:
                    lines.append(f"{indent}else if ({subject} == {const_val}) {{")
                
                for bl in body_lines:
                    if context_type == 'return':
                        lines.append(f"{indent}    return {bl};")
                    elif context_type == 'cast_return':
                        lines.append(f"{indent}    return {bl};")
                    elif context_type == 'assign':
                        lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        lines.append(f"{indent}    {bl}")
                
                lines.append(f"{indent}}}")
                first = False
            
            elif case['type'] == 'default':
                body_lines = self.clean_body(case['body'], case['arrow_type'])
                
                if first:
                    lines.append(f"{indent}{{")
                else:
                    lines.append(f"{indent}else {{")
                
                for bl in body_lines:
                    if context_type == 'return':
                        lines.append(f"{indent}    return {bl};")
                    elif context_type == 'cast_return':
                        lines.append(f"{indent}    return {bl};")
                    elif context_type == 'assign':
                        lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        lines.append(f"{indent}    {bl}")
                
                lines.append(f"{indent}}}")
        
        return '\n'.join(lines)
    
    def clean_body(self, body, arrow_type):
        """Clean case body."""
        body = body.strip()
        if not body:
            return []
        
        if arrow_type == '->':
            body = body.rstrip(';,')
            return [body] if body else []
        else:
            lines = []
            for line in body.split('\n'):
                line = line.strip()
                if line and line != 'break;':
                    lines.append(line)
            return lines
    
    def rewrite(self):
        """Main rewrite loop."""
        self.output = []
        self.pos = 0
        
        while self.pos < self.length:
            # Check for 'switch' keyword
            if self.match_keyword('switch'):
                switch_start = self.pos
                
                # Determine context BEFORE consuming 'switch'
                context_type, assign_target = self.find_switch_context(switch_start)
                
                # Get indentation
                line_start = self.content.rfind('\n', 0, switch_start) + 1
                indent = self.content[line_start:switch_start]
                
                # Parse the switch block
                saved_pos = self.pos
                switch_info = self.parse_switch_block()
                
                if switch_info and any(c['type'] == 'pattern' for c in switch_info['cases']):
                    # Has pattern matching - rewrite
                    new_code = self.generate_if_else(switch_info, context_type, assign_target, indent)
                    self.output.append(new_code)
                    # Position is already advanced by parse_switch_block
                else:
                    # No pattern matching - keep original
                    self.pos = saved_pos
                    # Copy until after the switch block
                    brace_count = 0
                    found_brace = False
                    while self.pos < self.length:
                        ch = self.consume()
                        self.output.append(ch)
                        if ch == '{':
                            brace_count += 1
                            found_brace = True
                        elif ch == '}':
                            brace_count -= 1
                            if found_brace and brace_count == 0:
                                break
                    # Check for semicolon
                    while self.peek().isspace():
                        self.output.append(self.consume())
                    if self.peek() == ';':
                        self.output.append(self.consume())
            else:
                self.output.append(self.consume())
        
        return ''.join(self.output)

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False
    
    original = content
    rewriter = SwitchRewriter(content)
    content = rewriter.rewrite()
    
    if content != original:
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Rewrote: {filepath}")
            return True
        except Exception as e:
            print(f"Error writing {filepath}: {e}")
            import traceback
            traceback.print_exc()
            return False
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        try:
            if process_file(filepath):
                fixed_count += 1
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
            import traceback
            traceback.print_exc()
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()